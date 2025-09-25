package com.location.client.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class RestDataSource implements DataSourceProvider {
  private final String baseUrl;
  private final CloseableHttpClient http = HttpClients.createDefault();
  private final ObjectMapper om = new ObjectMapper();
  private final AtomicReference<String> bearer = new AtomicReference<>();

  public RestDataSource(String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  @Override
  public String getLabel() {
    return "REST";
  }

  @Override
  public void resetDemoData() {
    // no-op (server will expose admin/reset endpoints in Diff 2 si besoin)
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
          result.add(
              new Models.Client(
                  client.path("id").asText(),
                  client.path("name").asText(),
                  client.path("billingEmail").asText()));
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
          String license = resource.path("licensePlate").asText();
          Integer color = resource.path("colorRgb").isInt() ? resource.path("colorRgb").asInt() : null;
          String agencyId = resource.path("agency").path("id").asText();
          result.add(new Models.Resource(id, name, license, color, agencyId));
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Models.Intervention> listInterventions(
      OffsetDateTime from, OffsetDateTime to, String resourceId) {
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
          result.add(new Models.Intervention(id, agency, resource, client, title, start, end));
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
      post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
      JsonNode node = executeForJson(post);
      String id = node.path("id").asText();
      return new Models.Intervention(
          id,
          intervention.agencyId(),
          intervention.resourceId(),
          intervention.clientId(),
          intervention.title(),
          intervention.start(),
          intervention.end());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void ensureLogin() {
    if (bearer.get() != null) return;
    String user = System.getenv().getOrDefault("LOCATION_DEMO_USER", "demo");
    String pass = System.getenv().getOrDefault("LOCATION_DEMO_PASSWORD", "demo");
    try {
      String url = baseUrl + "/auth/login";
      HttpPost post = new HttpPost(url);
      ObjectNode payload = om.createObjectNode();
      payload.put("username", user);
      payload.put("password", pass);
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
    return http.execute(
        req,
        res -> {
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

  private static String encode(String value) {
    return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  @Override
  public void close() throws Exception {
    http.close();
  }
}
