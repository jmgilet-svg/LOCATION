package com.location.client.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
    // Diff 1: serveur n'expose pas encore /api/v1/agencies → on retourne liste vide avec connectivité testée via /auth & /api/system/ping
    ensureLogin();
    return Collections.emptyList();
  }

  @Override
  public List<Models.Client> listClients() {
    ensureLogin();
    return Collections.emptyList();
  }

  private void ensureLogin() {
    if (bearer.get() != null) return;
    String user = System.getenv().getOrDefault("LOCATION_DEMO_USER", "demo");
    String pass = System.getenv().getOrDefault("LOCATION_DEMO_PASSWORD", "demo");
    try {
      String url = baseUrl + "/auth/login";
      HttpPost post = new HttpPost(url);
      String json = "{\"username\":\"" + escape(user) + "\",\"password\":\"" + escape(pass) + "\"}";
      post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
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

  private static String escape(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  @Override
  public void close() throws Exception {
    http.close();
  }
}
