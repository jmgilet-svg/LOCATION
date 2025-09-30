package com.location.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.CommercialDocument;
import com.location.server.domain.Intervention;
import com.location.server.domain.Resource;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
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

  @Test
  void documentBindingsExposeAgencyClientAndTotals() {
    var agency = new Agency("A2", "Agence Sud");
    agency.setLegalFooter("SARL Agence Sud – RCS 123 456 789");
    agency.setIban("FR76 3000 6000 1111 2222 3333 444");
    var client =
        new Client(
            "C2",
            "Client Beta",
            "beta@example.test",
            "06 12 34 56 78",
            "10 Rue des Tests",
            "75000",
            "Paris",
            "FR123456789",
            "FR76 1234 5678 9000");
    var document =
        new CommercialDocument(
            "D1",
            CommercialDocument.DocType.ORDER,
            CommercialDocument.DocStatus.SENT,
            "CMD-2025-01",
            "Levage 90T",
            OffsetDateTime.of(2025, 2, 10, 0, 0, 0, 0, ZoneOffset.UTC),
            agency,
            client);
    document.setTotals(new BigDecimal("1200.00"), new BigDecimal("240.00"), new BigDecimal("1440.00"));
    document.setDelivered(true);
    document.setPaid(false);

    Map<String, String> bindings = templateService.documentBindings(document);

    assertThat(bindings)
        .containsEntry("agencyName", "Agence Sud")
        .containsEntry("agencyLegalFooter", "SARL Agence Sud – RCS 123 456 789")
        .containsEntry("agencyIban", "FR76 3000 6000 1111 2222 3333 444")
        .containsEntry("clientName", "Client Beta")
        .containsEntry("clientPhone", "06 12 34 56 78")
        .containsEntry("clientEmail", "beta@example.test")
        .containsEntry("docStatus", "SENT")
        .containsEntry("docDelivered", "true")
        .containsEntry("docPaid", "false")
        .containsEntry("totalHt", "1200.00")
        .containsEntry("totalVat", "240.00")
        .containsEntry("totalTtc", "1440.00");
  }
}
