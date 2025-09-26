package com.location.client.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class RestDataSource implements DataSourceProvider {
  private final CloseableHttpClient http = HttpClients.createDefault();
  private final ObjectMapper om = new ObjectMapper();
  private final AtomicReference<String> bearer = new AtomicReference<>();

  private String baseUrl;
  private String username;
  private String password;

  public RestDataSource(String baseUrl) {
    this(baseUrl, System.getenv().getOrDefault("LOCATION_DEMO_USER", "demo"), System.getenv().getOrDefault("LOCATION_DEMO_PASSWORD", "demo"));
  }

  public RestDataSource(String baseUrl, String username, String password) {
    configure(baseUrl, username, password);
  }

  public synchronized void configure(String baseUrl, String username, String password) {
    this.baseUrl = normalize(baseUrl);
    this.username = username != null && !username.isBlank() ? username : "demo";
    this.password = password != null ? password : "demo";
    bearer.set(null);
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
  public List<Models.Agency> listAgencies() {
    try {
      ensureLogin();
      JsonNode node = executeForJson(new HttpGet(baseUrl + "/api/v1/agencies"));
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
      JsonNode node = executeForJson(new HttpGet(baseUrl + "/api/v1/clients"));
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
  public List<Models.Resource> listResources() {
    try {
      ensureLogin();
      JsonNode node = executeForJson(new HttpGet(baseUrl + "/api/v1/resources"));
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
      JsonNode node = executeForJson(new HttpGet(url.toString()));
      List<Models.Intervention> result = new ArrayList<>();
      if (node.isArray()) {
        for (JsonNode intervention : node) {
          String id = intervention.path("id").asText();
          String title = intervention.path("title").asText();
          String agency = intervention.path("agencyId").asText();
          String resource = intervention.path("resourceId").asText();
          String client = intervention.path("clientId").asText();
          java.time.Instant start = java.time.Instant.parse(intervention.path("start").asText());
          java.time.Instant end = java.time.Instant.parse(intervention.path("end").asText());
          JsonNode notesNode = intervention.path("notes");
          String notes = notesNode.isMissingNode() || notesNode.isNull() ? null : notesNode.asText();
          result.add(new Models.Intervention(id, agency, resource, client, title, start, end, notes));
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
      HttpPost post = new HttpPost(baseUrl + "/api/v1/interventions");
      ObjectNode payload = om.createObjectNode();
      payload.put("agencyId", intervention.agencyId());
      payload.put("resourceId", intervention.resourceId());
      payload.put("clientId", intervention.clientId());
      payload.put("title", intervention.title());
      payload.put("start", OffsetDateTime.ofInstant(intervention.start(), ZoneOffset.UTC).toString());
      payload.put("end", OffsetDateTime.ofInstant(intervention.end(), ZoneOffset.UTC).toString());
      if (intervention.notes() != null) {
        payload.put("notes", intervention.notes());
      } else {
        payload.putNull("notes");
      }
      post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
      JsonNode node = executeForJson(post);
      String id = node.path("id").asText();
      JsonNode notesNode = node.path("notes");
      String notes = notesNode.isMissingNode() || notesNode.isNull() ? null : notesNode.asText();
      return new Models.Intervention(
          id,
          intervention.agencyId(),
          intervention.resourceId(),
          intervention.clientId(),
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
      HttpPut put = new HttpPut(baseUrl + "/api/v1/interventions/" + encodeSegment(intervention.id()));
      ObjectNode payload = om.createObjectNode();
      payload.put("agencyId", intervention.agencyId());
      payload.put("resourceId", intervention.resourceId());
      payload.put("clientId", intervention.clientId());
      payload.put("title", intervention.title());
      payload.put("start", OffsetDateTime.ofInstant(intervention.start(), ZoneOffset.UTC).toString());
      payload.put("end", OffsetDateTime.ofInstant(intervention.end(), ZoneOffset.UTC).toString());
      if (intervention.notes() != null) {
        payload.put("notes", intervention.notes());
      } else {
        payload.putNull("notes");
      }
      put.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
      JsonNode node = executeForJson(put);
      return new Models.Intervention(
          node.path("id").asText(),
          node.path("agencyId").asText(),
          node.path("resourceId").asText(),
          node.path("clientId").asText(),
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
      HttpDelete delete = new HttpDelete(baseUrl + "/api/v1/interventions/" + encodeSegment(id));
      http.execute(
          delete,
          res -> {
            int code = res.getCode();
            if (code != 204) {
              throw new IOException("DELETE non OK: " + code);
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
      JsonNode node = executeForJson(new HttpGet(url.toString()));
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
      HttpPost post = new HttpPost(baseUrl + "/api/v1/unavailabilities");
      ObjectNode payload = om.createObjectNode();
      payload.put("resourceId", unavailability.resourceId());
      payload.put(
          "start", OffsetDateTime.ofInstant(unavailability.start(), ZoneOffset.UTC).toString());
      payload.put(
          "end", OffsetDateTime.ofInstant(unavailability.end(), ZoneOffset.UTC).toString());
      payload.put("reason", unavailability.reason());
      post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
      JsonNode node = executeForJson(post);
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
      JsonNode node = executeForJson(new HttpGet(url));
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
      HttpPost post = new HttpPost(baseUrl + "/api/v1/recurring-unavailabilities");
      ObjectNode payload = om.createObjectNode();
      payload.put("resourceId", recurringUnavailability.resourceId());
      payload.put("dayOfWeek", recurringUnavailability.dayOfWeek().name());
      payload.put("start", recurringUnavailability.start().toString());
      payload.put("end", recurringUnavailability.end().toString());
      payload.put("reason", recurringUnavailability.reason());
      post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
      JsonNode node = executeForJson(post);
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
      HttpGet get = new HttpGet(url);
      if (bearer.get() != null) {
        get.addHeader("Authorization", "Bearer " + bearer.get());
      }
      return http.execute(
          get,
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
            throw new IOException("HTTP " + sc + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Path downloadClientsCsv(Path target) {
    try {
      ensureLogin();
      HttpGet get = new HttpGet(baseUrl + "/api/v1/clients/csv");
      if (bearer.get() != null) {
        get.addHeader("Authorization", "Bearer " + bearer.get());
      }
      return http.execute(
          get,
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
            throw new IOException("HTTP " + sc + " → " + body);
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
      HttpGet get = new HttpGet(url.toString());
      if (bearer.get() != null) {
        get.addHeader("Authorization", "Bearer " + bearer.get());
      }
      return http.execute(
          get,
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
            throw new IOException("HTTP " + sc + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Path downloadInterventionPdf(String interventionId, Path target) {
    try {
      ensureLogin();
      HttpGet get = new HttpGet(baseUrl + "/api/v1/interventions/" + encodeSegment(interventionId) + "/pdf");
      if (bearer.get() != null) {
        get.addHeader("Authorization", "Bearer " + bearer.get());
      }
      return http.execute(
          get,
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
            throw new IOException("HTTP " + sc + " → " + body);
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
      HttpPost post =
          new HttpPost(baseUrl + "/api/v1/interventions/" + encodeSegment(interventionId) + "/email");
      if (bearer.get() != null) {
        post.addHeader("Authorization", "Bearer " + bearer.get());
      }
      ObjectNode payload = om.createObjectNode();
      payload.put("to", to);
      if (subject != null) {
        payload.put("subject", subject);
      }
      if (message != null) {
        payload.put("message", message);
      }
      post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
      http.execute(
          post,
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
            throw new IOException("HTTP " + code + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public java.util.Map<String, Boolean> getServerFeatures() {
    try {
      ensureLogin();
      HttpGet get = new HttpGet(baseUrl + "/api/v1/system/features");
      if (bearer.get() != null) {
        get.addHeader("Authorization", "Bearer " + bearer.get());
      }
      return http.execute(
          get,
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
            throw new IOException("HTTP " + sc + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
      HttpGet get = new HttpGet(url.toString());
      if (bearer.get() != null) {
        get.addHeader("Authorization", "Bearer " + bearer.get());
      }
      return http.execute(
          get,
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
            throw new IOException("HTTP " + sc + " → " + body);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void ensureLogin() {
    if (bearer.get() != null) return;
    try {
      String url = baseUrl + "/auth/login";
      HttpPost post = new HttpPost(url);
      ObjectNode payload = om.createObjectNode();
      payload.put("username", username);
      payload.put("password", password);
      post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
      JsonNode node = executeForJson(post);
      String token = node.path("token").asText(null);
      if (token == null) throw new IllegalStateException("Token manquant dans /auth/login");
      bearer.set(token);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private JsonNode executeForJson(HttpUriRequestBase req) throws IOException {
    if (bearer.get() != null) req.addHeader("Authorization", "Bearer " + bearer.get());
    return http.execute(req, res -> {
      int sc = res.getCode();
      HttpEntity entity = res.getEntity();
      String body = entity == null ? "" : new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
      if (sc >= 200 && sc < 300) {
        if (body.isEmpty()) return om.nullNode();
        return om.readTree(body);
      }
      throw new IOException("HTTP " + sc + " → " + body);
    });
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
