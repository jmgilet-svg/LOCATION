package com.location.server.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Resource;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.ResourceRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(InterventionService.class)
class InterventionServiceTest {

  @Autowired AgencyRepository agencyRepository;
  @Autowired ClientRepository clientRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired InterventionService service;

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
  void conflictDetectionThrowsException() {
    OffsetDateTime start = OffsetDateTime.of(2025, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC);
    service.create(AGENCY_ID, RESOURCE_ID, CLIENT_ID, "OK", start, start.plusHours(2));

    assertThrows(
        AssignmentConflictException.class,
        () -> service.create(AGENCY_ID, RESOURCE_ID, CLIENT_ID, "KO", start.plusHours(1), start.plusHours(3)));
  }
}
