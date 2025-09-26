package com.location.server.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Resource;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.ResourceRepository;
import com.location.server.repo.UnavailabilityRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({InterventionService.class, UnavailabilityService.class})
class UnavailabilityConflictTest {

  @Autowired AgencyRepository agencyRepository;
  @Autowired ClientRepository clientRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired UnavailabilityRepository unavailabilityRepository;
  @Autowired InterventionService interventionService;
  @Autowired UnavailabilityService unavailabilityService;

  private static final String AGENCY_ID = "A";
  private static final String CLIENT_ID = "C";
  private static final String RESOURCE_ID = "R";

  @BeforeEach
  void setUp() {
    agencyRepository.save(new Agency(AGENCY_ID, "Agence"));
    clientRepository.save(new Client(CLIENT_ID, "Client", "client@example.test"));
    resourceRepository.save(
        new Resource(RESOURCE_ID, "Camion", "AA-000-AA", null, agencyRepository.getReferenceById(AGENCY_ID)));
  }

  @Test
  void creatingInterventionOverUnavailabilityFails() {
    OffsetDateTime start = OffsetDateTime.of(2025, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC);
    unavailabilityService.create(RESOURCE_ID, start, start.plusHours(4), "Maintenance");

    assertThrows(
        AssignmentConflictException.class,
        () ->
            interventionService.create(
                AGENCY_ID, RESOURCE_ID, CLIENT_ID, "Interv", start.plusHours(1), start.plusHours(2)));
  }

  @Test
  void creatingUnavailabilityOverInterventionFails() {
    OffsetDateTime start = OffsetDateTime.of(2025, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC);
    interventionService.create(AGENCY_ID, RESOURCE_ID, CLIENT_ID, "Interv", start, start.plusHours(2));

    assertThrows(
        AssignmentConflictException.class,
        () -> unavailabilityService.create(RESOURCE_ID, start.plusHours(1), start.plusHours(3), "Panne"));
  }
}
