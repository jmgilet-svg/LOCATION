package com.location.server.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.CommercialDocumentRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CommercialDocumentControllerTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired AgencyRepository agencyRepository;
  @Autowired ClientRepository clientRepository;
  @Autowired CommercialDocumentRepository documentRepository;

  private String agencyId;
  private String clientId;

  @BeforeEach
  void setup() {
    documentRepository.deleteAll();
    clientRepository.deleteAll();
    agencyRepository.deleteAll();

    agencyId = UUID.randomUUID().toString();
    clientId = UUID.randomUUID().toString();
    agencyRepository.save(new Agency(agencyId, "Test Agency"));
    clientRepository.save(new Client(clientId, "Test Client", "client@test.tld"));
  }

  @Test
  void fullLifecycle() throws Exception {
    String createBody =
        objectMapper.writeValueAsString(
            Map.of(
                "type", "QUOTE",
                "agencyId", agencyId,
                "clientId", clientId,
                "title", "Levage chantier"));
    String created =
        mvc.perform(post("/api/v1/docs").contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("QUOTE"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode createdNode = objectMapper.readTree(created);
    String documentId = createdNode.path("id").asText();

    String updateBody =
        "{" +
            "\"reference\":\"DV-001\"," +
            "\"title\":\"Levage grue\"," +
            "\"lines\":[{" +
            "\"designation\":\"Prestation\"," +
            "\"quantity\":2.5," +
            "\"unitPrice\":120.0," +
            "\"vatRate\":20.0" +
            "}]" +
            "}";

    String updated =
        mvc.perform(put("/api/v1/docs/" + documentId).contentType(MediaType.APPLICATION_JSON).content(updateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalTtc").value(org.hamcrest.Matchers.greaterThan(0.0)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode updatedNode = objectMapper.readTree(updated);
    assertThat(updatedNode.path("totalHt").asDouble()).isGreaterThan(0.0);

    mvc.perform(get("/api/v1/docs/" + documentId + "/pdf"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));

    mvc.perform(post("/api/v1/docs/" + documentId + "/transition").param("to", "ORDER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("ORDER"));
  }
}
