package com.location.server.api.v1;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class InterventionNotesWebTest {

  @Autowired MockMvc mvc;
  @Autowired AgencyRepository agencyRepository;
  @Autowired ClientRepository clientRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired DriverRepository driverRepository;
  @Autowired InterventionRepository interventionRepository;
  @Autowired UnavailabilityRepository unavailabilityRepository;

  private OffsetDateTime start;
  private OffsetDateTime end;
  private String interventionId;
  private String driverId;

  @BeforeEach
  void setUp() {
    interventionRepository.deleteAll();
    unavailabilityRepository.deleteAll();
    resourceRepository.deleteAll();
    clientRepository.deleteAll();
    agencyRepository.deleteAll();

    Agency agency = agencyRepository.save(new Agency("A", "Agence"));
    Client client = clientRepository.save(new Client("C", "Client", "client@example.test"));
    Resource resource =
        resourceRepository.save(new Resource("R", "Ressource", "AB-123-CD", null, agency));
    Driver driver = driverRepository.save(new Driver("D", "Driver", "driver@example.test"));
    start = OffsetDateTime.of(2025, 1, 10, 8, 0, 0, 0, ZoneOffset.UTC);
    end = start.plusHours(2);
    Intervention intervention =
        new Intervention("I", "Titre", start, end, agency, resource, client, driver, null);
    interventionId = interventionRepository.save(intervention).getId();
    driverId = driver.getId();
  }

  @Test
  void update_roundTripNotes() throws Exception {
    String payload =
        "{" +
        "\"agencyId\":\"A\"," +
        "\"resourceId\":\"R\"," +
        "\"clientId\":\"C\"," +
        "\"driverId\":\"" + driverId + "\"," +
        "\"title\":\"MAJ\"," +
        "\"start\":\"" + start.plusHours(1).toString() + "\"," +
        "\"end\":\"" + end.plusHours(1).toString() + "\"," +
        "\"notes\":\"Ces notes ✅\"}";

    mvc.perform(
            put("/api/v1/interventions/" + interventionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value("Ces notes ✅"));
  }
}
