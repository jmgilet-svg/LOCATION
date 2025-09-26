package com.location.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Intervention;
import com.location.server.domain.Resource;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TemplateService.class)
class TemplateServiceTest {

  @Autowired private AgencyRepository agencyRepository;
  @Autowired private ClientRepository clientRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private InterventionRepository interventionRepository;
  @Autowired private TemplateService templateService;

  private Intervention intervention;

  @BeforeEach
  void setUp() {
    var agency = agencyRepository.save(new Agency("A", "Agence Nord"));
    var client = clientRepository.save(new Client("C", "Client Alpha", "alpha@example.test"));
    var resource =
        resourceRepository.save(new Resource("R", "Grue X", "PLATE", null, agency));
    intervention =
        interventionRepository.save(
            new Intervention(
                "I",
                "Levage",
                OffsetDateTime.of(2025, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                agency,
                resource,
                client,
                "Note"));
  }

  @Test
  void replacesVariables() {
    String template = "{{agencyName}} / {{clientName}} / {{interventionTitle}} / {{start}}-{{end}}";
    String rendered = templateService.renderBody(template, intervention);
    assertThat(rendered)
        .contains("Agence Nord")
        .contains("Client Alpha")
        .contains("Levage")
        .contains("2025-01-01 08:00")
        .contains("2025-01-01 10:00");
  }
}
