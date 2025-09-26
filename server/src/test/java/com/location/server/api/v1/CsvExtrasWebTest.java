package com.location.server.api.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Resource;
import com.location.server.domain.Unavailability;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CsvExtrasWebTest {
  @Autowired MockMvc mvc;
  @Autowired AgencyRepository agencyRepository;
  @Autowired ClientRepository clientRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired InterventionRepository interventionRepository;
  @Autowired UnavailabilityRepository unavailabilityRepository;

  @BeforeEach
  void setup() {
    interventionRepository.deleteAll();
    unavailabilityRepository.deleteAll();
    resourceRepository.deleteAll();
    clientRepository.deleteAll();
    agencyRepository.deleteAll();

    var agency = agencyRepository.save(new Agency("AG", "Agence"));
    clientRepository.save(new Client("CL", "Client", "client@example.test"));
    var resource = resourceRepository.save(new Resource("RS", "Ressource", "XX-000", null, agency));
    unavailabilityRepository.save(
        new Unavailability(
            "UN",
            resource,
            OffsetDateTime.of(2025, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC),
            OffsetDateTime.of(2025, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC),
            "Test"));
  }

  @Test
  void clientsCsv_hasAttachmentDisposition() throws Exception {
    mvc.perform(get("/api/v1/clients/csv"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
  }

  @Test
  void unavailabilitiesCsv_hasAttachmentDisposition() throws Exception {
    mvc.perform(get("/api/v1/unavailabilities/csv"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
  }
}
