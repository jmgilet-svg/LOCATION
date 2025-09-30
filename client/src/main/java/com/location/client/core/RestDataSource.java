package com.location.client.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
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
  public Models.EmailTemplate getAgencyEmailTemplate(String agencyId, String templateKey) {
    String key = DataSourceProvider.normalizeTemplateKey(templateKey);
    String targetAgency =
        agencyId != null && !agencyId.isBlank() ? agencyId : getCurrentAgencyId();
    if (targetAgency == null || targetAgency.isBlank()) {
      throw new IllegalArgumentException("Agence requise pour récupérer le modèle e-mail");
    }
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () ->
                  new HttpGet(
                      baseUrl
                          + "/api/v1/agencies/"
                          + encodeSegment(targetAgency)
                          + "/email-template"));
      String subject =
          node.path("subject").isMissingNode() || node.path("subject").isNull()
              ? ""
              : node.path("subject").asText();
      String body =
          node.path("body").isMissingNode() || node.path("body").isNull()
              ? ""
              : node.path("body").asText();
      return new Models.EmailTemplate(key, subject, body);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.EmailTemplate updateAgencyEmailTemplate(
      String agencyId, String templateKey, String subject, String html) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("Sujet requis");
    }
    if (html == null || html.isBlank()) {
      throw new IllegalArgumentException("Corps e-mail requis");
    }
    String key = DataSourceProvider.normalizeTemplateKey(templateKey);
    String targetAgency =
        agencyId != null && !agencyId.isBlank() ? agencyId : getCurrentAgencyId();
    if (targetAgency == null || targetAgency.isBlank()) {
      throw new IllegalArgumentException("Agence requise pour enregistrer le modèle e-mail");
    }
    try {
      ensureLogin();
      ObjectNode payload = om.createObjectNode();
      payload.put("subject", subject);
      payload.put("body", html);
      JsonNode node =
          executeForJson(
              () -> {
                HttpPut put =
                    new HttpPut(
                        baseUrl
                            + "/api/v1/agencies/"
                            + encodeSegment(targetAgency)
                            + "/email-template");
                put.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return put;
              });
      String savedSubject =
          node.path("subject").isMissingNode() || node.path("subject").isNull()
              ? ""
              : node.path("subject").asText();
      String savedBody =
          node.path("body").isMissingNode() || node.path("body").isNull()
              ? ""
              : node.path("body").asText();
      return new Models.EmailTemplate(key, savedSubject, savedBody);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void emailBulk(java.util.List<String> ids, String toOverride) {
    if (ids == null || ids.isEmpty()) {
      throw new IllegalArgumentException("Aucun identifiant d'intervention fourni");
    }
    try {
      ensureLogin();
      ObjectNode payload = om.createObjectNode();
      ArrayNode array = payload.putArray("ids");
      for (String id : ids) {
        if (id != null && !id.isBlank()) {
          array.add(id);
        }
      }
      if (array.isEmpty()) {
        throw new IllegalArgumentException("Aucun identifiant d'intervention valide");
      }
      if (toOverride == null || toOverride.isBlank()) {
        payload.putNull("toOverride");
      } else {
        payload.put("toOverride", toOverride);
      }
      execute(
          () -> {
            HttpPost post =
                new HttpPost(baseUrl + "/api/v1/interventions/email-bulk");
            post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
            return post;
          },
          res -> {
            int sc = res.getCode();
            EntityUtils.consumeQuietly(res.getEntity());
            if (sc != 202) {
              throw httpError(sc, "Email bulk HTTP " + sc);
            }
            return null;
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void emailBulk(java.util.List<String> recipients, String subject, String html) {
    throw new RuntimeException("emailBulk non disponible sur ce backend (démo).");
  }

  @Override
  public List<Models.Agency> listAgencies() {
    try {
      ensureLogin();
      JsonNode node = executeForJson(() -> new HttpGet(baseUrl + "/api/v1/agencies"));
      List<Models.Agency> result = new ArrayList<>();
      if (node.isArray()) {
        for (JsonNode agency : node) {
          result.add(
              new Models.Agency(
                  agency.path("id").asText(),
                  agency.path("name").asText(),
                  textOrNull(agency, "legalFooter"),
                  textOrNull(agency, "iban"),
                  textOrNull(agency, "logoDataUri")));
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.Agency getAgency(String id) {
    if (id == null || id.isBlank()) {
      return null;
    }
    try {
      ensureLogin();
      JsonNode node = executeForJson(() -> new HttpGet(baseUrl + "/api/v1/agencies/" + encodeSegment(id)));
      return new Models.Agency(
          node.path("id").asText(),
          node.path("name").asText(),
          textOrNull(node, "legalFooter"),
          textOrNull(node, "iban"),
          textOrNull(node, "logoDataUri"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.Agency saveAgency(Models.Agency agency) {
    if (agency == null) {
      throw new IllegalArgumentException("Agence requise");
    }
    String name = agency.name();
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Nom de l'agence requis");
    }
    try {
      ensureLogin();
      ObjectNode payload = om.createObjectNode();
      if (agency.id() != null && !agency.id().isBlank()) {
        payload.put("id", agency.id());
      }
      payload.put("name", name);
      if (agency.legalFooter() != null) {
        payload.put("legalFooter", agency.legalFooter());
      }
      if (agency.iban() != null) {
        payload.put("iban", agency.iban());
      }
      if (agency.logoDataUri() != null) {
        payload.put("logoDataUri", agency.logoDataUri());
      }
      JsonNode node =
          executeForJson(
              () -> {
                HttpPost post = new HttpPost(baseUrl + "/api/v1/agencies");
                post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return post;
              });
      return new Models.Agency(
          node.path("id").asText(),
          node.path("name").asText(),
          textOrNull(node, "legalFooter"),
          textOrNull(node, "iban"),
          textOrNull(node, "logoDataUri"));
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
          Models.Client parsed = readClient(client);
          if (parsed != null) {
            result.add(parsed);
          }
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.Client saveClient(Models.Client client) {
    if (client == null) {
      throw new IllegalArgumentException("Client requis");
    }
    if (client.name() == null || client.name().isBlank()) {
      throw new IllegalArgumentException("Nom du client requis");
    }
    try {
      ensureLogin();
      ObjectNode payload = om.createObjectNode();
      if (client.id() != null && !client.id().isBlank()) {
        payload.put("id", client.id());
      }
      payload.put("name", client.name());
      if (client.email() != null) {
        payload.put("email", client.email());
      }
      if (client.phone() != null) {
        payload.put("phone", client.phone());
      }
      if (client.address() != null) {
        payload.put("address", client.address());
      }
      if (client.zip() != null) {
        payload.put("zip", client.zip());
      }
      if (client.city() != null) {
        payload.put("city", client.city());
      }
      if (client.vatNumber() != null) {
        payload.put("vatNumber", client.vatNumber());
      }
      if (client.iban() != null) {
        payload.put("iban", client.iban());
      }
      JsonNode node =
          executeForJson(
              () -> {
                HttpPost post = new HttpPost(baseUrl + "/api/v1/clients");
                post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return post;
              });
      Models.Client saved = readClient(node);
      if (saved == null) {
        throw new RuntimeException("Réponse invalide lors de l'enregistrement du client");
      }
      return saved;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteClient(String clientId) {
    if (clientId == null || clientId.isBlank()) {
      return;
    }
    try {
      ensureLogin();
      execute(
          () -> new HttpDelete(baseUrl + "/api/v1/clients/" + encodeSegment(clientId)),
          res -> {
            int code = res.getCode();
            EntityUtils.consumeQuietly(res.getEntity());
            if (code < 200 || code >= 300) {
              throw httpError(code, "Suppression client HTTP " + code);
            }
            return null;
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Models.Contact> listContacts(String clientId) {
    if (clientId == null || clientId.isBlank()) {
      return List.of();
    }
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () ->
                  new HttpGet(
                      baseUrl
                          + "/api/v1/clients/"
                          + encodeSegment(clientId)
                          + "/contacts"));
      List<Models.Contact> result = new ArrayList<>();
      if (node.isArray()) {
        for (JsonNode contact : node) {
          Models.Contact parsed = readContact(contact);
          if (parsed != null) {
            result.add(parsed);
          }
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.Contact saveContact(Models.Contact contact) {
    if (contact == null) {
      throw new IllegalArgumentException("Contact requis");
    }
    if (contact.clientId() == null || contact.clientId().isBlank()) {
      throw new IllegalArgumentException("Client requis pour le contact");
    }
    try {
      ensureLogin();
      ObjectNode payload = om.createObjectNode();
      if (contact.id() != null && !contact.id().isBlank()) {
        payload.put("id", contact.id());
      }
      payload.put("clientId", contact.clientId());
      if (contact.firstName() != null) {
        payload.put("firstName", contact.firstName());
      }
      if (contact.lastName() != null) {
        payload.put("lastName", contact.lastName());
      }
      if (contact.email() != null) {
        payload.put("email", contact.email());
      }
      if (contact.phone() != null) {
        payload.put("phone", contact.phone());
      }
      JsonNode node =
          executeForJson(
              () -> {
                HttpPost post =
                    new HttpPost(
                        baseUrl
                            + "/api/v1/clients/"
                            + encodeSegment(contact.clientId())
                            + "/contacts");
                post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return post;
              });
      Models.Contact saved = readContact(node);
      if (saved == null) {
        throw new RuntimeException("Réponse invalide lors de l'enregistrement du contact");
      }
      return saved;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteContact(String contactId) {
    if (contactId == null || contactId.isBlank()) {
      return;
    }
    try {
      ensureLogin();
      execute(
          () -> new HttpDelete(baseUrl + "/api/v1/contacts/" + encodeSegment(contactId)),
          res -> {
            int code = res.getCode();
            EntityUtils.consumeQuietly(res.getEntity());
            if (code < 200 || code >= 300) {
              throw httpError(code, "Suppression contact HTTP " + code);
            }
            return null;
          });
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
  public List<Models.ResourceType> listResourceTypes() {
    try {
      ensureLogin();
      JsonNode node = executeForJson(() -> new HttpGet(baseUrl + "/api/v1/resource-types"));
      List<Models.ResourceType> result = new ArrayList<>();
      if (node.isArray()) {
        for (JsonNode type : node) {
          String id = type.path("id").asText();
          String name = type.path("name").asText();
          String icon = type.path("iconName").asText();
          result.add(new Models.ResourceType(id, name, icon));
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Models.ResourceType saveResourceType(Models.ResourceType resourceType) {
    if (resourceType == null) {
      throw new IllegalArgumentException("Type de ressource requis");
    }
    try {
      ensureLogin();
      ObjectNode payload = om.createObjectNode();
      if (resourceType.id() != null && !resourceType.id().isBlank()) {
        payload.put("id", resourceType.id());
      }
      payload.put("name", resourceType.name());
      payload.put("iconName", resourceType.iconName());
      JsonNode node =
          executeForJson(
              () -> {
                HttpPost post = new HttpPost(baseUrl + "/api/v1/resource-types");
                post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return post;
              });
      return new Models.ResourceType(
          node.path("id").asText(), node.path("name").asText(), node.path("iconName").asText());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteResourceType(String id) {
    if (id == null || id.isBlank()) {
      return;
    }
    try {
      ensureLogin();
      execute(
          () -> new HttpDelete(baseUrl + "/api/v1/resource-types/" + encodeSegment(id)),
          res -> {
            int code = res.getCode();
            EntityUtils.consumeQuietly(res.getEntity());
            if (code >= 200 && code < 300) {
              return null;
            }
            throw httpError(code, "HTTP " + code + " → suppression type");
          });
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
          List<String> resources = readResourceIds(intervention);
          String client = intervention.path("clientId").asText();
          JsonNode driverNode = intervention.path("driverId");
          String driver = driverNode.isMissingNode() || driverNode.isNull() ? null : driverNode.asText();
          java.time.Instant start = java.time.Instant.parse(intervention.path("start").asText());
          java.time.Instant end = java.time.Instant.parse(intervention.path("end").asText());
          JsonNode notesNode = intervention.path("notes");
          String notes = notesNode.isMissingNode() || notesNode.isNull() ? null : notesNode.asText();
          JsonNode internalNotesNode = intervention.path("internalNotes");
          String internalNotes =
              internalNotesNode.isMissingNode() || internalNotesNode.isNull()
                  ? null
                  : internalNotesNode.asText();
          JsonNode priceNode = intervention.path("price");
          Double price = priceNode.isMissingNode() || priceNode.isNull() ? null : priceNode.asDouble();
          result.add(
              new Models.Intervention(
                  id,
                  agency,
                  resources,
                  client,
                  driver,
                  title,
                  start,
                  end,
                  notes,
                  internalNotes,
                  price));
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
                String primaryResource = intervention.resourceId();
                if (primaryResource != null && !primaryResource.isBlank()) {
                  payload.put("resourceId", primaryResource);
                } else {
                  payload.putNull("resourceId");
                }
                if (intervention.resourceIds() != null && !intervention.resourceIds().isEmpty()) {
                  ArrayNode array = om.createArrayNode();
                  for (String rid : intervention.resourceIds()) {
                    if (rid != null && !rid.isBlank()) {
                      array.add(rid);
                    }
                  }
                  payload.set("resourceIds", array);
                }
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
                if (intervention.internalNotes() != null) {
                  payload.put("internalNotes", intervention.internalNotes());
                } else {
                  payload.putNull("internalNotes");
                }
                if (intervention.price() != null) {
                  payload.put("price", intervention.price());
                } else {
                  payload.putNull("price");
                }
                post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return post;
              });
      String id = node.path("id").asText();
      JsonNode notesNode = node.path("notes");
      String notes = notesNode.isMissingNode() || notesNode.isNull() ? null : notesNode.asText();
      JsonNode internalNotesNode = node.path("internalNotes");
      String internalNotes =
          internalNotesNode.isMissingNode() || internalNotesNode.isNull()
              ? intervention.internalNotes()
              : internalNotesNode.asText();
      JsonNode priceNode = node.path("price");
      Double price =
          priceNode.isMissingNode() || priceNode.isNull() ? intervention.price() : priceNode.asDouble();
      return new Models.Intervention(
          id,
          intervention.agencyId(),
          readResourceIds(node),
          intervention.clientId(),
          intervention.driverId(),
          intervention.title(),
          intervention.start(),
          intervention.end(),
          notes,
          internalNotes,
          price);
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
                String primaryResource = intervention.resourceId();
                if (primaryResource != null && !primaryResource.isBlank()) {
                  payload.put("resourceId", primaryResource);
                } else {
                  payload.putNull("resourceId");
                }
                if (intervention.resourceIds() != null && !intervention.resourceIds().isEmpty()) {
                  ArrayNode array = om.createArrayNode();
                  for (String rid : intervention.resourceIds()) {
                    if (rid != null && !rid.isBlank()) {
                      array.add(rid);
                    }
                  }
                  payload.set("resourceIds", array);
                }
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
                if (intervention.internalNotes() != null) {
                  payload.put("internalNotes", intervention.internalNotes());
                } else {
                  payload.putNull("internalNotes");
                }
                if (intervention.price() != null) {
                  payload.put("price", intervention.price());
                } else {
                  payload.putNull("price");
                }
                put.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
                return put;
              });
      return new Models.Intervention(
          node.path("id").asText(),
          node.path("agencyId").asText(),
          readResourceIds(node),
          node.path("clientId").asText(),
          node.path("driverId").isMissingNode() || node.path("driverId").isNull()
              ? null
              : node.path("driverId").asText(),
          node.path("title").asText(),
          java.time.Instant.parse(node.path("start").asText()),
          java.time.Instant.parse(node.path("end").asText()),
          node.path("notes").isMissingNode() || node.path("notes").isNull()
              ? null
              : node.path("notes").asText(),
          node.path("internalNotes").isMissingNode() || node.path("internalNotes").isNull()
              ? null
              : node.path("internalNotes").asText(),
          node.path("price").isMissingNode() || node.path("price").isNull()
              ? null
              : node.path("price").asDouble());
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

  private List<String> readResourceIds(JsonNode node) {
    List<String> resources = new ArrayList<>();
    JsonNode resourcesNode = node.path("resourceIds");
    if (resourcesNode.isArray()) {
      for (JsonNode r : resourcesNode) {
        if (!r.isNull()) {
          String value = r.asText();
          if (value != null && !value.isBlank()) {
            resources.add(value);
          }
        }
      }
    }
    if (resources.isEmpty()) {
      JsonNode single = node.path("resourceId");
      if (!single.isMissingNode() && !single.isNull()) {
        String value = single.asText();
        if (value != null && !value.isBlank()) {
          resources.add(value);
        }
      }
    }
    return resources;
  }

  private Models.Client readClient(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    return new Models.Client(
        textOrNull(node, "id"),
        textOrNull(node, "name"),
        textOrNull(node, "email"),
        textOrNull(node, "phone"),
        textOrNull(node, "address"),
        textOrNull(node, "zip"),
        textOrNull(node, "city"),
        textOrNull(node, "vatNumber"),
        textOrNull(node, "iban"));
  }

  private Models.Contact readContact(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    return new Models.Contact(
        textOrNull(node, "id"),
        textOrNull(node, "clientId"),
        textOrNull(node, "firstName"),
        textOrNull(node, "lastName"),
        textOrNull(node, "email"),
        textOrNull(node, "phone"));
  }

  @Override
  public String getResourceTypeForResource(String resourceId) {
    if (resourceId == null || resourceId.isBlank()) {
      return null;
    }
    try {
      ensureLogin();
      JsonNode node =
          executeForJson(
              () ->
                  new HttpGet(
                      baseUrl + "/api/v1/resources/" + encodeSegment(resourceId) + "/type"));
      JsonNode value = node.path("resourceTypeId");
      if (value.isMissingNode() || value.isNull()) {
        return null;
      }
      String text = value.asText();
      return text.isBlank() ? null : text;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setResourceTypeForResource(String resourceId, String resourceTypeId) {
    if (resourceId == null || resourceId.isBlank()) {
      throw new IllegalArgumentException("Ressource requise");
    }
    try {
      ensureLogin();
      String url = baseUrl + "/api/v1/resources/" + encodeSegment(resourceId) + "/type";
      if (resourceTypeId == null || resourceTypeId.isBlank()) {
        execute(
            () -> new HttpDelete(url),
            res -> {
              int code = res.getCode();
              EntityUtils.consumeQuietly(res.getEntity());
              if (code >= 200 && code < 300) {
                return null;
              }
              throw httpError(code, "HTTP " + code + " → suppression type ressource");
            });
        return;
      }
      ObjectNode payload = om.createObjectNode();
      payload.put("resourceTypeId", resourceTypeId);
      executeForJson(
          () -> {
            HttpPut put = new HttpPut(url);
            put.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
            return put;
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
      String url1 = baseUrl + "/api/v1/recurring-unavailabilities";
      if (resourceId != null && !resourceId.isBlank()) {
        url1 += "?resourceId=" + encode(resourceId);
      }
      String url = url1;
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
      String url1 = baseUrl + "/api/v1/resources/csv";
      if (tags != null && !tags.isBlank()) {
    	  url1 += "?tags=" + encode(tags);
      }
      String url = url1;
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
                payload.put("delivered", document.delivered());
                payload.put("paid", document.paid());
                ArrayNode lines = payload.putArray("lines");
                for (Models.DocLine line : document.lines()) {
                  ObjectNode node1 = lines.addObject();
                  node1.put("designation", line.designation());
                  node1.put("quantity", line.quantity());
                  node1.put("unitPrice", line.unitPrice());
                  node1.put("vatRate", line.vatRate());
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
  public void emailDoc(String id, String to, String subject, String message, boolean attachPdf) {
    try {
      ensureLogin();
      execute(
          () -> {
            HttpPost post = new HttpPost(baseUrl + "/api/v1/docs/" + encodeSegment(id) + "/email");
            ObjectNode payload = om.createObjectNode();
            payload.put("to", to);
            payload.put("attachPdf", attachPdf);
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
        return new Models.EmailTemplate(docType, "", "");
      }
      return new Models.EmailTemplate(
          docType, node.path("subject").asText(""), node.path("body").asText(""));
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
      return new Models.EmailTemplate(
          docType, node.path("subject").asText(""), node.path("body").asText(""));
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
  public void emailDocsBatch(
      java.util.List<String> ids, String to, String subject, String message, boolean attachPdf) {
    try {
      ensureLogin();
      execute(
          () -> {
            HttpPost post = new HttpPost(baseUrl + "/api/v1/docs/email-batch");
            ObjectNode payload = om.createObjectNode();
            ArrayNode arr = payload.putArray("ids");
            ids.forEach(arr::add);
            payload.put("to", to);
            payload.put("attachPdf", attachPdf);
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

  private Models.Agency parseAgency(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return new Models.Agency(
        textOrNull(node, "id"),
        textOrNull(node, "name"),
        textOrNull(node, "legalFooter"),
        textOrNull(node, "iban"),
        textOrNull(node, "logoDataUri"));
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
        node.path("delivered").asBoolean(false),
        node.path("paid").asBoolean(false),
        java.util.List.copyOf(lines));
  }

  private static String textOrNull(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    String text = value.asText();
    return text == null || text.isBlank() ? null : text;
  }

  private static void putNullable(ObjectNode node, String field, String value) {
    if (value == null || value.isBlank()) {
      node.putNull(field);
    } else {
      node.put(field, value);
    }
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

  public void uploadPlanningPngForPdf(
      Path png,
      String title,
      String page,
      String orientation,
      Path logoPng,
      String agency,
      String period,
      String recapText,
      Path targetPdf)
      throws IOException {
    String url = baseUrl + "/api/v1/planning/pdf";
    HttpPost post = new HttpPost(url);
    applyHeaders(post);
    byte[] bytes = Files.readAllBytes(png);
    MultipartEntityBuilder builder =
        MultipartEntityBuilder.create()
            .setCharset(StandardCharsets.UTF_8)
            .addBinaryBody("image", bytes, ContentType.IMAGE_PNG, png.getFileName().toString());
    if (title != null && !title.isBlank()) {
      builder.addTextBody("title", title, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
    }
    builder.addTextBody(
        "page",
        page == null ? "auto" : page,
        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
    builder.addTextBody(
        "orientation",
        orientation == null ? "auto" : orientation,
        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
    if (agency != null && !agency.isBlank()) {
      builder.addTextBody(
          "agency", agency, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
    }
    if (period != null && !period.isBlank()) {
      builder.addTextBody(
          "period", period, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
    }
    if (recapText != null && !recapText.isBlank()) {
      builder.addTextBody(
          "recapText", recapText, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
    }
    if (logoPng != null) {
      byte[] logoBytes = Files.readAllBytes(logoPng);
      builder.addBinaryBody("logo", logoBytes, ContentType.IMAGE_PNG, logoPng.getFileName().toString());
    }
    post.setEntity(builder.build());
    try (CloseableHttpResponse response = http.execute(post)) {
      int statusCode = response.getCode();
      if (statusCode != 200) {
        throw new IOException("HTTP " + statusCode);
      }
      HttpEntity entity = response.getEntity();
      if (entity == null) {
        throw new IOException("Réponse vide");
      }
      try (InputStream in = entity.getContent()) {
        Files.copy(in, targetPdf, StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }
}
