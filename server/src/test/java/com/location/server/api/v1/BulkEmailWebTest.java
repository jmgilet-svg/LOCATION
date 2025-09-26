package com.location.server.api.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Intervention;
import com.location.server.domain.Resource;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BulkEmailWebTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AgencyRepository agencyRepository;
  @Autowired private ClientRepository clientRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private InterventionRepository interventionRepository;

  private String intervention1;
  private String intervention2;

  @BeforeEach
  void setUp() {
    interventionRepository.deleteAll();
    resourceRepository.deleteAll();
    clientRepository.deleteAll();
    agencyRepository.deleteAll();

    var agency = agencyRepository.save(new Agency("A", "Agence"));
    agency.setEmailSubjectTemplate("Intervention {{interventionTitle}}");
    agency.setEmailBodyTemplate("Bonjour {{clientName}}");
    agencyRepository.save(agency);

    var client = clientRepository.save(new Client("C", "Client", "client@example.test"));
    var resource = resourceRepository.save(new Resource("R", "Ressource", "PLATE", null, agency));

    intervention1 =
        interventionRepository
            .save(
                new Intervention(
                    "I1",
                    "Titre1",
                    OffsetDateTime.of(2025, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                    OffsetDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                    agency,
                    resource,
                    client,
                    "Notes"))
            .getId();
    intervention2 =
        interventionRepository
            .save(
                new Intervention(
                    "I2",
                    "Titre2",
                    OffsetDateTime.of(2025, 1, 2, 8, 0, 0, 0, ZoneOffset.UTC),
                    OffsetDateTime.of(2025, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC),
                    agency,
                    resource,
                    client,
                    "Notes"))
            .getId();
  }

  @Test
  void bulkReturnsAccepted() throws Exception {
    String body = "{\"ids\":[\"" + intervention1 + "\",\"" + intervention2 + "\"],\"toOverride\":\"client@example.test\"}";
    mockMvc
        .perform(post("/api/v1/interventions/email-bulk").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isAccepted());
  }
}
