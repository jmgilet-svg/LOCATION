package com.location.server.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Driver;
import com.location.server.domain.Resource;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.DriverRepository;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
class ApiV1ControllerTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired AgencyRepository agencyRepository;
  @Autowired ClientRepository clientRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired DriverRepository driverRepository;
  @Autowired InterventionRepository interventionRepository;

  @BeforeEach
  void setUp() {
    interventionRepository.deleteAll();
    if (agencyRepository.count() == 0) {
      Agency agency = agencyRepository.save(new Agency("A", "Agence"));
      clientRepository.save(new Client("C", "Client", "client@example.test"));
      resourceRepository.save(new Resource("R", "Camion", "", null, agency));
      driverRepository.save(new Driver("D", "Chauffeur", "driver@example.test"));
    }
  }

  @Test
  void getAgenciesReturnsOk() throws Exception {
    mvc.perform(get("/api/v1/agencies")).andExpect(status().isOk());
  }

  @Test
  void postInterventionConflictReturns409() throws Exception {
    var first = objectMapper.createObjectNode();
    first.put("agencyId", "A");
    first.put("resourceId", "R");
    first.put("clientId", "C");
    first.put("driverId", "D");
    first.put("title", "Intervention 1");
    first.put("start", "2025-01-01T08:00:00Z");
    first.put("end", "2025-01-01T10:00:00Z");

    mvc.perform(
            post("/api/v1/interventions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(first)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists());

    var conflict = objectMapper.createObjectNode();
    conflict.put("agencyId", "A");
    conflict.put("resourceId", "R");
    conflict.put("clientId", "C");
    conflict.put("driverId", "D");
    conflict.put("title", "Intervention 2");
    conflict.put("start", "2025-01-01T09:00:00Z");
    conflict.put("end", "2025-01-01T11:00:00Z");

    mvc.perform(
            post("/api/v1/interventions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(conflict)))
        .andExpect(status().isConflict());
  }
}
