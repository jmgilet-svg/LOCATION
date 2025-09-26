package com.location.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.location.server.domain.Agency;
import com.location.server.domain.Resource;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ResourceRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({UnavailabilityService.class, UnavailabilityQueryService.class})
class RecurringUnavailabilityExpansionTest {

  @Autowired AgencyRepository agencyRepository;
  @Autowired ResourceRepository resourceRepository;
  @Autowired UnavailabilityService unavailabilityService;
  @Autowired UnavailabilityQueryService unavailabilityQueryService;

  private String resourceId;

  @BeforeEach
  void setUp() {
    Agency agency = agencyRepository.save(new Agency("AG", "Agence"));
    Resource resource =
        resourceRepository.save(new Resource("RES", "Ressource", "XX", null, agency));
    resourceId = resource.getId();
  }

  @Test
  void expandsRecurringOccurrencesWithinWindow() {
    LocalDate monday = LocalDate.of(2025, 1, 6); // Monday
    unavailabilityService.createRecurring(
        resourceId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), "Routine");
    OffsetDateTime from = monday.minusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime to = monday.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

    var spans = unavailabilityQueryService.search(from, to, resourceId);

    assertThat(spans)
        .anyMatch(
            span ->
                span.recurring()
                    && span.start().toLocalDate().equals(monday)
                    && span.resourceId().equals(resourceId));
  }
}

