package com.location.server.service;

import com.location.server.domain.Resource;
import com.location.server.domain.Unavailability;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import com.location.server.repo.UnavailabilityRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnavailabilityService {
  private final UnavailabilityRepository unavailabilityRepository;
  private final ResourceRepository resourceRepository;
  private final InterventionRepository interventionRepository;

  public UnavailabilityService(
      UnavailabilityRepository unavailabilityRepository,
      ResourceRepository resourceRepository,
      InterventionRepository interventionRepository) {
    this.unavailabilityRepository = unavailabilityRepository;
    this.resourceRepository = resourceRepository;
    this.interventionRepository = interventionRepository;
  }

  @Transactional
  public Unavailability create(String resourceId, OffsetDateTime start, OffsetDateTime end, String reason) {
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException("start must be before end");
    }
    if (unavailabilityRepository.existsOverlap(resourceId, start, end)) {
      throw new AssignmentConflictException("Chevauchement avec une indisponibilité existante");
    }
    if (interventionRepository.existsOverlap(resourceId, start, end)) {
      throw new AssignmentConflictException("Chevauchement avec une intervention existante");
    }
    Resource resource = resourceRepository.findById(resourceId).orElseThrow();
    Unavailability unavailability =
        new Unavailability(UUID.randomUUID().toString(), resource, start, end, reason);
    return unavailabilityRepository.save(unavailability);
  }
}
