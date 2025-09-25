package com.location.server.service;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Intervention;
import com.location.server.domain.Resource;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterventionService {
  private final InterventionRepository interventionRepository;
  private final AgencyRepository agencyRepository;
  private final ResourceRepository resourceRepository;
  private final ClientRepository clientRepository;

  public InterventionService(
      InterventionRepository interventionRepository,
      AgencyRepository agencyRepository,
      ResourceRepository resourceRepository,
      ClientRepository clientRepository) {
    this.interventionRepository = interventionRepository;
    this.agencyRepository = agencyRepository;
    this.resourceRepository = resourceRepository;
    this.clientRepository = clientRepository;
  }

  @Transactional
  public Intervention create(
      String agencyId,
      String resourceId,
      String clientId,
      String title,
      OffsetDateTime start,
      OffsetDateTime end) {
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException("start must be before end");
    }
    if (interventionRepository.existsOverlap(resourceId, start, end)) {
      throw new AssignmentConflictException(
          "Intervention en conflit pour la ressource " + resourceId);
    }
    Agency agency = agencyRepository.findById(agencyId).orElseThrow();
    Resource resource = resourceRepository.findById(resourceId).orElseThrow();
    Client client = clientRepository.findById(clientId).orElseThrow();
    Intervention intervention =
        new Intervention(UUID.randomUUID().toString(), title, start, end, agency, resource, client);
    return interventionRepository.save(intervention);
  }
}
