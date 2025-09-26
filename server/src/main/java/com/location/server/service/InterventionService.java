package com.location.server.service;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Intervention;
import com.location.server.domain.Resource;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import com.location.server.repo.UnavailabilityRepository;
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
  private final UnavailabilityRepository unavailabilityRepository;

  public InterventionService(
      InterventionRepository interventionRepository,
      AgencyRepository agencyRepository,
      ResourceRepository resourceRepository,
      ClientRepository clientRepository,
      UnavailabilityRepository unavailabilityRepository) {
    this.interventionRepository = interventionRepository;
    this.agencyRepository = agencyRepository;
    this.resourceRepository = resourceRepository;
    this.clientRepository = clientRepository;
    this.unavailabilityRepository = unavailabilityRepository;
  }

  @Transactional
  public Intervention create(
      String agencyId,
      String resourceId,
      String clientId,
      String title,
      OffsetDateTime start,
      OffsetDateTime end,
      String notes) {
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException("start must be before end");
    }
    if (interventionRepository.existsOverlap(resourceId, start, end)) {
      throw new AssignmentConflictException(
          "Intervention en conflit pour la ressource " + resourceId);
    }
    if (unavailabilityRepository.existsOverlap(resourceId, start, end)) {
      throw new AssignmentConflictException("Ressource indisponible sur le créneau");
    }
    Agency agency = agencyRepository.findById(agencyId).orElseThrow();
    Resource resource = resourceRepository.findById(resourceId).orElseThrow();
    Client client = clientRepository.findById(clientId).orElseThrow();
    Intervention intervention =
        new Intervention(
            UUID.randomUUID().toString(), title, start, end, agency, resource, client, notes);
    return interventionRepository.save(intervention);
  }

  @Transactional
  public Intervention update(
      String id,
      String agencyId,
      String resourceId,
      String clientId,
      String title,
      OffsetDateTime start,
      OffsetDateTime end,
      String notes) {
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException("start must be before end");
    }
    if (interventionRepository.existsOverlapExcluding(id, resourceId, start, end)) {
      throw new AssignmentConflictException(
          "Intervention en conflit pour la ressource " + resourceId);
    }
    if (unavailabilityRepository.existsOverlap(resourceId, start, end)) {
      throw new AssignmentConflictException("Ressource indisponible sur le créneau");
    }
    Intervention intervention = interventionRepository.findById(id).orElseThrow();
    if (!intervention.getAgency().getId().equals(agencyId)) {
      intervention.setAgency(agencyRepository.findById(agencyId).orElseThrow());
    }
    if (!intervention.getResource().getId().equals(resourceId)) {
      intervention.setResource(resourceRepository.findById(resourceId).orElseThrow());
    }
    if (!intervention.getClient().getId().equals(clientId)) {
      intervention.setClient(clientRepository.findById(clientId).orElseThrow());
    }
    intervention.setTitle(title);
    intervention.setStart(start);
    intervention.setEnd(end);
    intervention.setNotes(notes);
    return interventionRepository.save(intervention);
  }

  @Transactional
  public void delete(String id) {
    interventionRepository.deleteById(id);
  }
}
