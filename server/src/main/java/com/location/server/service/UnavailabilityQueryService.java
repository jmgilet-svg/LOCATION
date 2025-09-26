package com.location.server.service;

import com.location.server.domain.RecurringUnavailability;
import com.location.server.domain.Unavailability;
import com.location.server.repo.RecurringUnavailabilityRepository;
import com.location.server.repo.UnavailabilityRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UnavailabilityQueryService {

  public record Span(
      String id,
      String resourceId,
      OffsetDateTime start,
      OffsetDateTime end,
      String reason,
      boolean recurring) {}

  private final UnavailabilityRepository unavailabilityRepository;
  private final RecurringUnavailabilityRepository recurringUnavailabilityRepository;

  public UnavailabilityQueryService(
      UnavailabilityRepository unavailabilityRepository,
      RecurringUnavailabilityRepository recurringUnavailabilityRepository) {
    this.unavailabilityRepository = unavailabilityRepository;
    this.recurringUnavailabilityRepository = recurringUnavailabilityRepository;
  }

  public List<Span> search(OffsetDateTime from, OffsetDateTime to, String resourceId) {
    List<Span> result = new ArrayList<>();
    for (Unavailability unavailability :
        unavailabilityRepository.search(from, to, resourceId)) {
      result.add(
          new Span(
              unavailability.getId(),
              unavailability.getResource().getId(),
              unavailability.getStart(),
              unavailability.getEnd(),
              unavailability.getReason(),
              false));
    }
    if (from == null || to == null) {
      return result;
    }
    List<RecurringUnavailability> recurring =
        recurringUnavailabilityRepository.search(resourceId);
    LocalDate day = from.toLocalDate();
    LocalDate end = to.toLocalDate();
    while (!day.isAfter(end)) {
      DayOfWeek dow = day.getDayOfWeek();
      for (RecurringUnavailability ru : recurring) {
        if (ru.getDayOfWeek() != dow) {
          continue;
        }
        OffsetDateTime start =
            day.atTime(ru.getStartTime()).atOffset(ZoneOffset.UTC);
        OffsetDateTime endTime = day.atTime(ru.getEndTime()).atOffset(ZoneOffset.UTC);
        if (endTime.isAfter(from) && start.isBefore(to)) {
          result.add(
              new Span(
                  "ru:" + ru.getId() + ":" + day,
                  ru.getResource().getId(),
                  start,
                  endTime,
                  ru.getReason(),
                  true));
        }
      }
      day = day.plusDays(1);
    }
    return result;
  }
}

