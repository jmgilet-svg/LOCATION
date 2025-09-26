package com.location.server.api.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Driver;
import com.location.server.domain.Intervention;
import com.location.server.domain.Resource;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.DriverRepository;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import com.location.server.repo.UnavailabilityRepository;
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
class InterventionUpdateWebTest {

  @Autowired MockMvc mockMvc;
  @Autowired AgencyRepository agencyRepository;
  @Autowired ClientRepository clientRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired DriverRepository driverRepository;
  @Autowired InterventionRepository interventionRepository;
  @Autowired UnavailabilityRepository unavailabilityRepository;
  private String agencyId;
  private String clientId;
  private String resourceId;
  private String driverId;
  private String interventionId;

  private final OffsetDateTime base = OffsetDateTime.of(2025, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC);

  @BeforeEach
  void setup() {
    interventionRepository.deleteAll();
    unavailabilityRepository.deleteAll();
    resourceRepository.deleteAll();
    clientRepository.deleteAll();
    agencyRepository.deleteAll();

    Agency agency = agencyRepository.save(new Agency("A", "Agence"));
    Client client = clientRepository.save(new Client("C", "Client", "client@example.test"));
    Resource resource = resourceRepository.save(new Resource("R", "Ressource", "XX-000-YY", null, agency));
    Driver driver = driverRepository.save(new Driver("D", "Driver", "driver@example.test"));
    Intervention intervention =
        interventionRepository.save(
            new Intervention("I1", "Titre", base, base.plusHours(2), agency, resource, client, driver, null));

    agencyId = agency.getId();
    clientId = client.getId();
    resourceId = resource.getId();
    driverId = driver.getId();
    interventionId = intervention.getId();
  }

  @Test
  void update_ok_then_delete_returns_204() throws Exception {
    String payload =
        """
            {"agencyId":"%s","resourceId":"%s","clientId":"%s","driverId":"%s","title":"MAJ","start":"%s","end":"%s"}
            """
            .formatted(agencyId, resourceId, clientId, driverId, base.plusHours(1), base.plusHours(3));

    mockMvc
        .perform(
            put("/api/v1/interventions/" + interventionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("MAJ"));

    mockMvc.perform(delete("/api/v1/interventions/" + interventionId)).andExpect(status().isNoContent());
  }

  @Test
  void update_conflict_on_overlap_returns_409() throws Exception {
    Agency agency = agencyRepository.getReferenceById(agencyId);
    Resource resource = resourceRepository.getReferenceById(resourceId);
    Client client = clientRepository.getReferenceById(clientId);
    Driver driver = driverRepository.getReferenceById(driverId);

    interventionRepository.save(
        new Intervention(
            "I2", "Autre", base.plusHours(2), base.plusHours(4), agency, resource, client, driver, null));

    String payload =
        """
            {"agencyId":"%s","resourceId":"%s","clientId":"%s","driverId":"%s","title":"MAJ","start":"%s","end":"%s"}
            """
            .formatted(agencyId, resourceId, clientId, driverId, base.plusHours(1), base.plusHours(3));

    mockMvc
        .perform(
            put("/api/v1/interventions/" + interventionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict());
  }
}
