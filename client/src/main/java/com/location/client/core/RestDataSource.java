package com.location.client.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

public class RestDataSource implements DataSourceProvider {
  private static final String DEFAULT_AGENCY_ID =
      System.getenv().getOrDefault("LOCATION_DEFAULT_AGENCY_ID", "A1");
  private final CloseableHttpClient http = HttpClients.createDefault();
  private final ObjectMapper om = new ObjectMapper();
  private final AtomicReference<String> bearer = new AtomicReference<>();
  private final AtomicBoolean pingThreadStarted = new AtomicBoolean();
  private final AtomicLong lastPingEpochMs = new AtomicLong();

  private static final String DEFAULT_USERNAME =
      System.getenv().getOrDefault("LOCATION_USERNAME", "demo");
  private static final String DEFAULT_PASSWORD =
      System.getenv().getOrDefault("LOCATION_PASSWORD", "demo");

  private String baseUrl;
  private volatile String username = DEFAULT_USERNAME;
  private volatile String password = DEFAULT_PASSWORD;
  private volatile String currentAgencyId = DEFAULT_AGENCY_ID;

  public RestDataSource(String baseUrl) {
    this(baseUrl, DEFAULT_USERNAME, DEFAULT_PASSWORD);
  }

  public RestDataSource(String baseUrl, String username, String password) {
    configure(baseUrl, username, password);
  }

  public synchronized void configure(String baseUrl, String username, String password) {
    this.baseUrl = normalize(baseUrl);
    this.username = username != null ? username : DEFAULT_USERNAME;
    this.password = password != null ? password : DEFAULT_PASSWORD;
    bearer.set(null);
    lastPingEpochMs.set(0L);
  }

  public synchronized void setCredentials(String username, String password) {
    configure(this.baseUrl, username, password);
  }

  private String normalize(String url) {
    if (url == null || url.isBlank()) {
      return "http://localhost:8080";
    }
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  @Override
  public String getLabel() {
    return "REST";
  }

  @Override
  public void resetDemoData() {
    // no-op pour REST
  }

  @Override
  public void resetDemo() {
    try {
      ensureLogin();
      execute(
          () -> new HttpPost(baseUrl + "/api/v1/demo/reset"),
          res -> {
            int sc = res.getCode();
            EntityUtils.consumeQuietly(res.getEntity());
            if (sc >= 200 && sc < 300) {
              return null;
            }
            if (sc == 404) {
              throw new RuntimeException(
                  "Réinitialisation démo indisponible côté backend (HTTP 404).");
            }
            throw httpError(sc, "HTTP " + sc + " → réinitialisation démo");
          });
    } catch (IOException e) {
      throw new RuntimeException("Réinitialisation démo indisponible: " + e.getMessage(), e);
    }
  }

  @Override
  public List<Models.Agency> listAgencies() {
    try {
      ensureLogin();
      JsonNode node = executeForJson(() -> new HttpGet(baseUrl + "/api/v1/agencies"));
      List<Models.Agency> result = new ArrayList<>();
      if (node.isArray()) {
        for (JsonNode agency : node) {
          result.add(new Models.Agency(agency.path("id").asText(), agency.path("name").asText()));
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Models.Client> listClients() {
    try {
      ensureLogin();
      JsonNode node = executeForJson(() -> new HttpGet(baseUrl + "/api/v1/clients"));
      List<Models.Client> result = new ArrayList<>();
      if (node.isArray()) {
        for (JsonNode client : node) {
          result.add(new Models.Client(client.path("id").asText(), client.path("name").asText(), client.path("billingEmail").asText()));
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getCurrentAgencyId() {
    return currentAgencyId;
  }

  @Override
  public void setCurrentAgencyId(String agencyId) {
    this.currentAgencyId = agencyId;
  }

  public void startPingThread() {
    if (!pingThreadStarted.compareAndSet(false, true)) {
      return;
    }
    Thread t =
        new Thread(
            () -> {
              while (!Thread.currentThread().isInterrupted()) {
                try {
                  ensureLogin();
                  ClassicHttpRequest request =
                      ClassicRequestBuilder.get(baseUrl + "/api/system/ping")
                          .addHeader("Accept", "text/event-stream")
                          .build();
                  applyHeaders(request);
                  http.execute(
                      request,
                      response -> {
                        int sc = response.getCode();
                        if (sc == 401) {
                          EntityUtils.consumeQuietly(response.getEntity());
                          throw new UnauthorizedException();
                        }
                        if (sc < 200 || sc >= 300) {
                          String body =
                              response.getEntity() == null
                                  ? ""
                                  : new String(
                                      response.getEntity().getContent().readAllBytes(),
                                      StandardCharsets.UTF_8);
                          throw httpError(sc, "SSE HTTP " + sc + (body.isEmpty() ? "" : " → " + body));
                        }
                        try (BufferedReader reader =
                            new BufferedReader(
                                new InputStreamReader(
                                    response.getEntity().getContent(), StandardCharsets.UTF_8))) {
                          String line;
                          while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data:")) {
                              lastPingEpochMs.set(System.currentTimeMillis());
                            }
                          }
                        }
                        return null;
                      });
                } catch (UnauthorizedException e) {
                  bearer.set(null);
                  lastPingEpochMs.set(0L);
                } catch (IOException e) {
                  lastPingEpochMs.set(0L);
                  try {
                    Thread.sleep(1000L);
                  } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                  }
                }
              }
            },
            "rest-sse-ping");
    t.setDaemon(true);
    t.start();
  }

  public long getLastPingEpochMs() {
    return lastPingEpochMs.get();
  }

  @Override
  public List<Models.Resource> listResources() {
    try {
      ensureLogin();
      JsonNode node = executeForJson(() -> new HttpGet(baseUrl + "/api/v1/resources"));
      List<Models.Resource> result = new ArrayList<>();
      if (node.isArray()) {
        for (JsonNode resource : node) {
          String id = resource.path("id").asText();
          String name = resource.path("name").asText();
          String license = resource.path("licensePlate").isMissingNode() ? null : resource.path("licensePlate").asText(null);
          Integer color = resource.path("colorRgb").isInt() ? resource.path("colorRgb").asInt() : null;
          String agencyId = resource.path("agency").path("id").asText();
          String tags = resource.path("tags").asText(null);
          Integer capacity = resource.path("capacityTons").isInt() ? resource.path("capacityTons").asInt() : null;
          result.add(new Models.Resource(id, name, license, color, agencyId, tags, capacity));
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Models.Intervention> listInterventions(OffsetDateTime from, OffsetDateTime to, String resourceId) {
    try {
      ensureLogin();
      StringBuilder url = new StringBuilder(baseUrl + "/api/v1/interventions");
      List<String> params = new ArrayList<>();
      if (from != null) {
        params.add("from=" + encode(from.toString()));
      }
      if (to != null) {
        params.add("to=" + encode(to.toString()));
      }
      if (resourceId != null) {
        params.add("resourceId=" + encode(resourceId));
      }
      if (!params.isEmpty()) {
        url.append('?').append(String.join("&", params));
      }
      JsonNode node = executeForJson(() -> new HttpGet(url.toString()));
      List<Models.Intervention> result = new ArrayList<>();
      if (node.isArray()) {
        for (JsonNode intervention : node) {
          String id = intervention.path("id").asText();
          String title = intervention.path("title").asText();
          String agency = intervention.path("agencyId").asText();
          String resource = intervention.path("resourceId").asText();
          String client = intervention.path("clientId").asText();
          JsonNode driverNode = intervention.path("driverId");
          String driver = driverNode.isMissingNode() || driverNode.isNull() ? null : driverNode.asText();
          java.time.Instant start = java.time.Instant.parse(intervention.path("start").asText());
          java.time.Instant end = java.time.Instant.parse(intervention.path("end").asText());
          JsonNode notesNode = intervention.path("notes");
          String notes = notesNode.isMissingNode() || notesNode.isNull() ? null : notesNode.asText();
          result.add(new Models.Intervention(id, agency, resource, client, driver, title, start, end, notes));
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.Intervention createIntervention(Models.Intervention intervention) {
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () -> {
                HttpPost post = new HttpPost(baseUrl + "/api/v1/interventions");
                ObjectNode payload = om.createObjectNode();
                payload.put("agencyId", intervention.agencyId());
                payload.put("resourceId", intervention.resourceId());
                payload.put("clientId", intervention.clientId());
                if (intervention.driverId() != null) {
                  payload.put("driverId", intervention.driverId());
                } else {
                  payload.putNull("driverId");
                }
                payload.put("title", intervention.title());
                payload.put(
                    "start", OffsetDateTime.ofInstant(intervention.start(), ZoneOffset.UTC).toString());
                payload.put(
                    "end", OffsetDateTime.ofInstant(intervention.end(), ZoneOffset.UTC).toString());
                if (intervention.notes() != null) {
                  payload.put("notes", intervention.notes());
                } else {
                  payload.putNull("notes");
                }
                post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return post;
              });
      String id = node.path("id").asText();
      JsonNode notesNode = node.path("notes");
      String notes = notesNode.isMissingNode() || notesNode.isNull() ? null : notesNode.asText();
      return new Models.Intervention(
          id,
          intervention.agencyId(),
          intervention.resourceId(),
          intervention.clientId(),
          intervention.driverId(),
          intervention.title(),
          intervention.start(),
          intervention.end(),
          notes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.Intervention updateIntervention(Models.Intervention intervention) {
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () -> {
                HttpPut put =
                    new HttpPut(
                        baseUrl + "/api/v1/interventions/" + encodeSegment(intervention.id()));
                ObjectNode payload = om.createObjectNode();
                payload.put("agencyId", intervention.agencyId());
                payload.put("resourceId", intervention.resourceId());
                payload.put("clientId", intervention.clientId());
                if (intervention.driverId() != null) {
                  payload.put("driverId", intervention.driverId());
                } else {
                  payload.putNull("driverId");
                }
                payload.put("title", intervention.title());
                payload.put(
                    "start", OffsetDateTime.ofInstant(intervention.start(), ZoneOffset.UTC).toString());
                payload.put(
                    "end", OffsetDateTime.ofInstant(intervention.end(), ZoneOffset.UTC).toString());
                if (intervention.notes() != null) {
                  payload.put("notes", intervention.notes());
                } else {
                  payload.putNull("notes");
                }
                put.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return put;
              });
      return new Models.Intervention(
          node.path("id").asText(),
          node.path("agencyId").asText(),
          node.path("resourceId").asText(),
          node.path("clientId").asText(),
          node.path("driverId").isMissingNode() || node.path("driverId").isNull()
              ? null
              : node.path("driverId").asText(),
          node.path("title").asText(),
          java.time.Instant.parse(node.path("start").asText()),
          java.time.Instant.parse(node.path("end").asText()),
          node.path("notes").isMissingNode() || node.path("notes").isNull()
              ? null
              : node.path("notes").asText());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteIntervention(String id) {
    try {
      ensureLogin();
      execute(
          () -> new HttpDelete(baseUrl + "/api/v1/interventions/" + encodeSegment(id)),
          res -> {
            int code = res.getCode();
            if (code != 204) {
              throw httpError(code, "DELETE non OK: " + code);
            }
            return null;
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Models.Unavailability> listUnavailabilities(
      OffsetDateTime from, OffsetDateTime to, String resourceId) {
    try {
      ensureLogin();
      StringBuilder url = new StringBuilder(baseUrl + "/api/v1/unavailabilities");
      List<String> params = new ArrayList<>();
      if (from != null) {
        params.add("from=" + encode(from.toString()));
      }
      if (to != null) {
        params.add("to=" + encode(to.toString()));
      }
      if (resourceId != null) {
        params.add("resourceId=" + encode(resourceId));
      }
      if (!params.isEmpty()) {
        url.append('?').append(String.join("&", params));
      }
      JsonNode node = executeForJson(() -> new HttpGet(url.toString()));
      List<Models.Unavailability> result = new ArrayList<>();
      if (node.isArray()) {
        for (JsonNode unav : node) {
          String id = unav.path("id").asText();
          String rid = unav.path("resourceId").asText();
          String reason = unav.path("reason").asText();
          java.time.Instant start = java.time.Instant.parse(unav.path("start").asText());
          java.time.Instant end = java.time.Instant.parse(unav.path("end").asText());
          boolean recurring = unav.path("recurring").asBoolean(false);
          result.add(new Models.Unavailability(id, rid, reason, start, end, recurring));
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.Unavailability createUnavailability(Models.Unavailability unavailability) {
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () -> {
                HttpPost post = new HttpPost(baseUrl + "/api/v1/unavailabilities");
                ObjectNode payload = om.createObjectNode();
                payload.put("resourceId", unavailability.resourceId());
                payload.put(
                    "start",
                    OffsetDateTime.ofInstant(unavailability.start(), ZoneOffset.UTC).toString());
                payload.put(
                    "end",
                    OffsetDateTime.ofInstant(unavailability.end(), ZoneOffset.UTC).toString());
                payload.put("reason", unavailability.reason());
                post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return post;
              });
      String id = node.path("id").asText();
      return new Models.Unavailability(
          id,
          unavailability.resourceId(),
          unavailability.reason(),
          unavailability.start(),
          unavailability.end(),
          false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Models.RecurringUnavailability> listRecurringUnavailabilities(String resourceId) {
    try {
      ensureLogin();
      String url = baseUrl + "/api/v1/recurring-unavailabilities";
      if (resourceId != null && !resourceId.isBlank()) {
        url += "?resourceId=" + encode(resourceId);
      }
      JsonNode node = executeForJson(() -> new HttpGet(url));
      List<Models.RecurringUnavailability> result = new ArrayList<>();
      if (node.isArray()) {
        for (JsonNode ru : node) {
          result.add(
              new Models.RecurringUnavailability(
                  ru.path("id").asText(),
                  ru.path("resourceId").asText(),
                  java.time.DayOfWeek.valueOf(ru.path("dayOfWeek").asText()),
                  java.time.LocalTime.parse(ru.path("start").asText()),
                  java.time.LocalTime.parse(ru.path("end").asText()),
                  ru.path("reason").asText()));
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.RecurringUnavailability createRecurringUnavailability(
      Models.RecurringUnavailability recurringUnavailability) {
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () -> {
                HttpPost post = new HttpPost(baseUrl + "/api/v1/recurring-unavailabilities");
                ObjectNode payload = om.createObjectNode();
                payload.put("resourceId", recurringUnavailability.resourceId());
                payload.put("dayOfWeek", recurringUnavailability.dayOfWeek().name());
                payload.put("start", recurringUnavailability.start().toString());
                payload.put("end", recurringUnavailability.end().toString());
                payload.put("reason", recurringUnavailability.reason());
                post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return post;
              });
      return new Models.RecurringUnavailability(
          node.path("id").asText(),
          node.path("resourceId").asText(),
          java.time.DayOfWeek.valueOf(node.path("dayOfWeek").asText()),
          java.time.LocalTime.parse(node.path("start").asText()),
          java.time.LocalTime.parse(node.path("end").asText()),
          node.path("reason").asText());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Path downloadResourcesCsv(String tags, Path target) {
    try {
      ensureLogin();
      String url = baseUrl + "/api/v1/resources/csv";
      if (tags != null && !tags.isBlank()) {
        url += "?tags=" + encode(tags);
      }
      return execute(
          () -> new HttpGet(url),
          res -> {
            int sc = res.getCode();
            HttpEntity entity = res.getEntity();
            if (sc >= 200 && sc < 300 && entity != null) {
              byte[] bytes = EntityUtils.toByteArray(entity);
              Files.write(target, bytes);
              return target;
            }
            String body =
                entity == null
                    ? ""
                    : new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
            throw httpError(sc, "HTTP " + sc + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Path downloadClientsCsv(Path target) {
    try {
      ensureLogin();
      return execute(
          () -> new HttpGet(baseUrl + "/api/v1/clients/csv"),
          res -> {
            int sc = res.getCode();
            HttpEntity entity = res.getEntity();
            if (sc >= 200 && sc < 300 && entity != null) {
              byte[] bytes = EntityUtils.toByteArray(entity);
              Files.write(target, bytes);
              return target;
            }
            String body =
                entity == null
                    ? ""
                    : new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
            throw httpError(sc, "HTTP " + sc + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Path downloadUnavailabilitiesCsv(
      OffsetDateTime from, OffsetDateTime to, String resourceId, Path target) {
    try {
      ensureLogin();
      StringBuilder url = new StringBuilder(baseUrl + "/api/v1/unavailabilities/csv");
      List<String> params = new ArrayList<>();
      if (from != null) {
        params.add("from=" + encode(from.toString()));
      }
      if (to != null) {
        params.add("to=" + encode(to.toString()));
      }
      if (resourceId != null && !resourceId.isBlank()) {
        params.add("resourceId=" + encode(resourceId));
      }
      if (!params.isEmpty()) {
        url.append('?').append(String.join("&", params));
      }
      return execute(
          () -> new HttpGet(url.toString()),
          res -> {
            int sc = res.getCode();
            HttpEntity entity = res.getEntity();
            if (sc >= 200 && sc < 300 && entity != null) {
              byte[] bytes = EntityUtils.toByteArray(entity);
              Files.write(target, bytes);
              return target;
            }
            String body =
                entity == null
                    ? ""
                    : new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
            throw httpError(sc, "HTTP " + sc + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Path downloadInterventionsCsv(OffsetDateTime from, OffsetDateTime to, Path target) {
    try {
      ensureLogin();
      String url =
          baseUrl
              + "/api/v1/interventions.csv?from="
              + encode(from.toString())
              + "&to="
              + encode(to.toString());
      return execute(
          () -> new HttpGet(url),
          res -> {
            int sc = res.getCode();
            HttpEntity entity = res.getEntity();
            if (sc >= 200 && sc < 300 && entity != null) {
              byte[] bytes = EntityUtils.toByteArray(entity);
              Files.write(target, bytes);
              return target;
            }
            String body =
                entity == null
                    ? ""
                    : new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
            throw httpError(sc, "HTTP " + sc + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Path downloadInterventionPdf(String interventionId, Path target) {
    try {
      ensureLogin();
      return execute(
          () ->
              new HttpGet(
                  baseUrl + "/api/v1/interventions/" + encodeSegment(interventionId) + "/pdf"),
          response -> {
            int sc = response.getCode();
            HttpEntity entity = response.getEntity();
            if (sc >= 200 && sc < 300 && entity != null) {
              byte[] bytes = EntityUtils.toByteArray(entity);
              Files.write(target, bytes);
              return target;
            }
            String body =
                entity == null
                    ? ""
                    : new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
            throw httpError(sc, "HTTP " + sc + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void emailInterventionPdf(String interventionId, String to, String subject, String message) {
    try {
      ensureLogin();
      if (to == null || to.isBlank()) {
        throw new IllegalArgumentException("Destinataire requis");
      }
      execute(
          () -> {
            HttpPost post =
                new HttpPost(
                    baseUrl
                        + "/api/v1/interventions/"
                        + encodeSegment(interventionId)
                        + "/email");
            ObjectNode payload = om.createObjectNode();
            payload.put("to", to);
            if (subject != null) {
              payload.put("subject", subject);
            }
            if (message != null) {
              payload.put("message", message);
            }
            post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
            return post;
          },
          res -> {
            int code = res.getCode();
            HttpEntity entity = res.getEntity();
            if (code == 202) {
              return null;
            }
            String body =
                entity == null
                    ? ""
                    : new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
            throw httpError(code, "HTTP " + code + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public java.util.Map<String, Boolean> getServerFeatures() {
    try {
      ensureLogin();
      return execute(
          () -> new HttpGet(baseUrl + "/api/v1/system/features"),
          res -> {
            int sc = res.getCode();
            HttpEntity entity = res.getEntity();
            String body =
                entity == null
                    ? ""
                    : new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
            if (sc >= 200 && sc < 300) {
              java.util.HashMap<String, Boolean> flags = new java.util.HashMap<>();
              if (body.isEmpty()) {
                return flags;
              }
              JsonNode node = om.readTree(body);
              node.fieldNames()
                  .forEachRemaining(field -> flags.put(field, node.path(field).asBoolean(false)));
              return flags;
            }
            throw httpError(sc, "HTTP " + sc + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public java.util.List<Models.Doc> listDocs(String type, String clientId) {
    try {
      ensureLogin();
      StringBuilder url = new StringBuilder(baseUrl + "/api/v1/docs");
      java.util.List<String> params = new java.util.ArrayList<>();
      if (type != null && !type.isBlank()) {
        params.add("type=" + encode(type));
      }
      if (clientId != null && !clientId.isBlank()) {
        params.add("clientId=" + encode(clientId));
      }
      if (!params.isEmpty()) {
        url.append('?').append(String.join("&", params));
      }
      JsonNode response = executeForJson(() -> new HttpGet(url.toString()));
      java.util.List<Models.Doc> docs = new java.util.ArrayList<>();
      if (response.isArray()) {
        for (JsonNode node : response) {
          docs.add(docFrom(node));
        }
      }
      return docs;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.Doc createDoc(String type, String agencyId, String clientId, String title) {
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () -> {
                HttpPost post = new HttpPost(baseUrl + "/api/v1/docs");
                ObjectNode payload = om.createObjectNode();
                payload.put("type", type);
                payload.put("agencyId", agencyId);
                payload.put("clientId", clientId);
                payload.put("title", title);
                post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return post;
              });
      return docFrom(node);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.Doc updateDoc(Models.Doc document) {
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () -> {
                HttpPut put =
                    new HttpPut(baseUrl + "/api/v1/docs/" + encodeSegment(document.id()));
                ObjectNode payload = om.createObjectNode();
                if (document.reference() == null || document.reference().isBlank()) {
                  payload.putNull("reference");
                } else {
                  payload.put("reference", document.reference());
                }
                payload.put("title", document.title());
                ArrayNode lines = payload.putArray("lines");
                for (Models.DocLine line : document.lines()) {
                  ObjectNode node = lines.addObject();
                  node.put("designation", line.designation());
                  node.put("quantity", line.quantity());
                  node.put("unitPrice", line.unitPrice());
                  node.put("vatRate", line.vatRate());
                }
                put.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return put;
              });
      return docFrom(node);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteDoc(String id) {
    try {
      ensureLogin();
      execute(
          () -> new HttpDelete(baseUrl + "/api/v1/docs/" + encodeSegment(id)),
          res -> {
            EntityUtils.consumeQuietly(res.getEntity());
            return null;
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.Doc transitionDoc(String id, String toType) {
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () ->
                  new HttpPost(
                      baseUrl + "/api/v1/docs/" + encodeSegment(id) + "/transition?to=" + encode(toType)));
      return docFrom(node);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public java.nio.file.Path downloadDocPdf(String id, java.nio.file.Path target) {
    try {
      ensureLogin();
      return execute(
          () -> new HttpGet(baseUrl + "/api/v1/docs/" + encodeSegment(id) + "/pdf"),
          res -> {
            if (res.getCode() != 200) {
              throw httpError(res.getCode(), "PDF HTTP " + res.getCode());
            }
            byte[] bytes = EntityUtils.toByteArray(res.getEntity());
            java.nio.file.Files.write(target, bytes);
            return target;
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void emailDoc(String id, String to, String subject, String message) {
    try {
      ensureLogin();
      execute(
          () -> {
            HttpPost post = new HttpPost(baseUrl + "/api/v1/docs/" + encodeSegment(id) + "/email");
            ObjectNode payload = om.createObjectNode();
            payload.put("to", to);
            if (subject == null || subject.isBlank()) {
              payload.putNull("subject");
            } else {
              payload.put("subject", subject);
            }
            if (message == null || message.isBlank()) {
              payload.putNull("message");
            } else {
              payload.put("message", message);
            }
            post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
            return post;
          },
          res -> {
            if (res.getCode() != 202) {
              throw httpError(res.getCode(), "Email HTTP " + res.getCode());
            }
            EntityUtils.consumeQuietly(res.getEntity());
            return null;
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public java.nio.file.Path downloadDocsCsv(String type, String clientId, java.nio.file.Path target) {
    try {
      ensureLogin();
      StringBuilder url = new StringBuilder(baseUrl + "/api/v1/docs.csv");
      java.util.List<String> params = new java.util.ArrayList<>();
      if (type != null && !type.isBlank()) {
        params.add("type=" + encode(type));
      }
      if (clientId != null && !clientId.isBlank()) {
        params.add("clientId=" + encode(clientId));
      }
      if (!params.isEmpty()) {
        url.append('?').append(String.join("&", params));
      }
      return execute(
          () -> new HttpGet(url.toString()),
          res -> {
            if (res.getCode() != 200) {
              throw httpError(res.getCode(), "CSV HTTP " + res.getCode());
            }
            byte[] bytes = EntityUtils.toByteArray(res.getEntity());
            java.nio.file.Files.write(target, bytes);
            return target;
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.EmailTemplate getEmailTemplate(String docType) {
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(() -> new HttpGet(baseUrl + "/api/v1/templates/" + encodeSegment(docType)));
      if (node.isNull() || node.isMissingNode()) {
        return new Models.EmailTemplate("", "");
      }
      return new Models.EmailTemplate(node.path("subject").asText(""), node.path("body").asText(""));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.EmailTemplate saveEmailTemplate(String docType, String subject, String body) {
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () -> {
                HttpPut put =
                    new HttpPut(baseUrl + "/api/v1/templates/" + encodeSegment(docType));
                var payload = om.createObjectNode();
                payload.put("subject", subject);
                payload.put("body", body);
                put.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return put;
              });
      return new Models.EmailTemplate(node.path("subject").asText(""), node.path("body").asText(""));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.DocTemplate getDocTemplate(String docType) {
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () -> new HttpGet(baseUrl + "/api/v1/templates/doc/" + encodeSegment(docType)));
      if (node.isNull() || node.isMissingNode()) {
        return new Models.DocTemplate("");
      }
      return new Models.DocTemplate(node.path("html").asText(""));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.DocTemplate saveDocTemplate(String docType, String html) {
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () -> {
                HttpPut put =
                    new HttpPut(baseUrl + "/api/v1/templates/doc/" + encodeSegment(docType));
                ObjectNode payload = om.createObjectNode();
                payload.put("html", html);
                put.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return put;
              });
      return new Models.DocTemplate(node.path("html").asText(""));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void emailDocsBatch(java.util.List<String> ids, String to, String subject, String message) {
    try {
      ensureLogin();
      execute(
          () -> {
            HttpPost post = new HttpPost(baseUrl + "/api/v1/docs/email-batch");
            ObjectNode payload = om.createObjectNode();
            ArrayNode arr = payload.putArray("ids");
            ids.forEach(arr::add);
            payload.put("to", to);
            if (subject == null || subject.isBlank()) {
              payload.putNull("subject");
            } else {
              payload.put("subject", subject);
            }
            if (message == null || message.isBlank()) {
              payload.putNull("message");
            } else {
              payload.put("message", message);
            }
            post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
            return post;
          },
          res -> {
            int code = res.getCode();
            if (code != 202) {
              String body =
                  res.getEntity() == null
                      ? ""
                      : new String(
                          res.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
              throw httpError(code, "Email batch HTTP " + code + " → " + body);
            }
            EntityUtils.consumeQuietly(res.getEntity());
            return null;
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.Resource saveResource(Models.Resource resource) {
    throw new RuntimeException("saveResource non disponible sur ce backend (démo).");
  }

  @Override
  public java.util.List<Models.Unavailability> listUnavailability(String resourceId) {
    throw new RuntimeException("listUnavailability non disponible sur ce backend (démo).");
  }

  @Override
  public Models.Unavailability saveUnavailability(Models.Unavailability unavailability) {
    throw new RuntimeException("saveUnavailability non disponible sur ce backend (démo).");
  }

  @Override
  public void deleteUnavailability(String id) {
    throw new RuntimeException("deleteUnavailability non disponible sur ce backend (démo).");
  }

  @Override
  public Models.RecurringUnavailability saveRecurringUnavailability(
      Models.RecurringUnavailability recurring) {
    throw new RuntimeException("saveRecurringUnavailability non disponible sur ce backend (démo).");
  }

  @Override
  public void deleteRecurringUnavailability(String id) {
    throw new RuntimeException("deleteRecurringUnavailability non disponible sur ce backend (démo).");
  }

  public Path downloadCsvInterventions(
      OffsetDateTime from,
      OffsetDateTime to,
      String resourceId,
      String clientId,
      String query,
      Path target) {
    try {
      ensureLogin();
      StringBuilder url = new StringBuilder(baseUrl + "/api/v1/interventions/csv");
      List<String> params = new ArrayList<>();
      if (from != null) {
        params.add("from=" + encode(from.toString()));
      }
      if (to != null) {
        params.add("to=" + encode(to.toString()));
      }
      if (resourceId != null && !resourceId.isBlank()) {
        params.add("resourceId=" + encode(resourceId));
      }
      if (clientId != null && !clientId.isBlank()) {
        params.add("clientId=" + encode(clientId));
      }
      if (query != null && !query.isBlank()) {
        params.add("q=" + encode(query));
      }
      if (!params.isEmpty()) {
        url.append('?').append(String.join("&", params));
      }
      return execute(
          () -> new HttpGet(url.toString()),
          response -> {
            int sc = response.getCode();
            HttpEntity entity = response.getEntity();
            if (sc >= 200 && sc < 300 && entity != null) {
              byte[] bytes = EntityUtils.toByteArray(entity);
              Files.write(target, bytes);
              return target;
            }
            String body =
                entity == null
                    ? ""
                    : new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
            throw httpError(sc, "HTTP " + sc + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void applyHeaders(ClassicHttpRequest request) {
    String token = bearer.get();
    if (token != null && !token.isBlank()) {
      request.addHeader("Authorization", "Bearer " + token);
    }
    if (currentAgencyId != null && !currentAgencyId.isBlank()) {
      request.addHeader("X-Agency-Id", currentAgencyId);
    }
  }

  private void ensureLogin() {
    if (bearer.get() != null) return;
    synchronized (bearer) {
      if (bearer.get() != null) return;
      try {
        HttpPost post = new HttpPost(baseUrl + "/auth/login");
        ObjectNode payload = om.createObjectNode();
        payload.put("username", username);
        payload.put("password", password);
        post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
        JsonNode node =
            withRetries(
                () ->
                    http.execute(
                        post,
                        res -> {
                          int sc = res.getCode();
                          HttpEntity entity = res.getEntity();
                          String body =
                              entity == null
                                  ? ""
                                  : new String(
                                      entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
                          if (sc >= 200 && sc < 300) {
                            if (body.isEmpty()) return om.nullNode();
                            return om.readTree(body);
                          }
                          throw httpError(sc, "HTTP " + sc + " → " + body);
                        }));
        String token = node.path("token").asText(null);
        if (token == null) {
          throw new IllegalStateException("Token manquant dans /auth/login");
        }
        bearer.set(token);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private JsonNode executeForJson(Supplier<HttpUriRequestBase> supplier) throws IOException {
    return execute(
        supplier,
        res -> {
          int sc = res.getCode();
          HttpEntity entity = res.getEntity();
          String body =
              entity == null
                  ? ""
                  : new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
          if (sc >= 200 && sc < 300) {
            if (body.isEmpty()) return om.nullNode();
            return om.readTree(body);
          }
          throw httpError(sc, "HTTP " + sc + " → " + body);
        });
  }

  private <T> T execute(Supplier<HttpUriRequestBase> supplier, ResponseHandler<T> handler)
      throws IOException {
    return withRetries(
        () ->
            withAutoRetry(
                () -> {
                  HttpUriRequestBase request = supplier.get();
                  applyHeaders(request);
                  return http.execute(
                      request,
                      response -> {
                        if (response.getCode() == 401) {
                          EntityUtils.consumeQuietly(response.getEntity());
                          throw new UnauthorizedException();
                        }
                        return handler.handle(response);
                      });
                }));
  }

  private <T> T withAutoRetry(IoSupplier<T> call) throws IOException {
    UnauthorizedException unauthorized = null;
    for (int attempt = 0; attempt < 2; attempt++) {
      ensureLogin();
      try {
        return call.get();
      } catch (IOException e) {
        if (e instanceof UnauthorizedException uex) {
          bearer.set(null);
          unauthorized = uex;
        } else {
          throw e;
        }
      }
    }
    if (unauthorized != null) {
      throw unauthorized;
    }
    throw new UnauthorizedException();
  }

  private <T> T withRetries(IoSupplier<T> call) throws IOException {
    int attempts = 0;
    long backoff = 250L;
    while (true) {
      try {
        return call.get();
      } catch (IOException e) {
        if (e instanceof UnauthorizedException) {
          throw e;
        }
        if (e instanceof HttpStatusException status
            && status.statusCode() >= 400
            && status.statusCode() < 500) {
          throw e;
        }
        attempts++;
        if (attempts >= 3) {
          throw e;
        }
        try {
          Thread.sleep(backoff);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted during retry", ie);
        }
        backoff = Math.min(backoff * 2, 2000L);
      }
    }
  }

  private static HttpStatusException httpError(int statusCode, String message) {
    return new HttpStatusException(statusCode, message);
  }

  @FunctionalInterface
  private interface IoSupplier<T> {
    T get() throws IOException;
  }

  @FunctionalInterface
  private interface ResponseHandler<T> {
    T handle(org.apache.hc.core5.http.ClassicHttpResponse response) throws IOException;
  }

  private static final class UnauthorizedException extends IOException {}

  private static final class HttpStatusException extends IOException {
    private final int statusCode;

    private HttpStatusException(int statusCode, String message) {
      super(message);
      this.statusCode = statusCode;
    }

    int statusCode() {
      return statusCode;
    }
  }

  private Models.Doc docFrom(JsonNode node) {
    java.util.List<Models.DocLine> lines = new java.util.ArrayList<>();
    JsonNode arr = node.path("lines");
    if (arr.isArray()) {
      for (JsonNode line : arr) {
        lines.add(
            new Models.DocLine(
                line.path("designation").asText(),
                line.path("quantity").asDouble(),
                line.path("unitPrice").asDouble(),
                line.path("vatRate").asDouble()));
      }
    }
    String reference = node.path("reference").isMissingNode() || node.path("reference").isNull()
        ? null
        : node.path("reference").asText();
    java.time.OffsetDateTime date = java.time.OffsetDateTime.parse(node.path("date").asText());
    return new Models.Doc(
        node.path("id").asText(),
        node.path("type").asText(),
        node.path("status").asText(),
        reference,
        node.path("title").asText(),
        node.path("agencyId").asText(),
        node.path("clientId").asText(),
        date,
        node.path("totalHt").asDouble(),
        node.path("totalVat").asDouble(),
        node.path("totalTtc").asDouble(),
        java.util.List.copyOf(lines));
  }

  private static String encodeSegment(String value) {
    return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private static String encode(String value) {
    return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  @Override
  public void close() throws Exception {
    http.close();
  }
}
