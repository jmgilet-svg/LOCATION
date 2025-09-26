package com.location.server.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Intervention;
import com.location.server.domain.Resource;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import com.location.server.repo.RecurringUnavailabilityRepository;
import com.location.server.repo.UnavailabilityRepository;
import com.location.server.service.MailGateway;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PdfAndEmailWebTest {

  @Autowired private MockMvc mvc;

  @Autowired private AgencyRepository agencyRepository;
  @Autowired private ClientRepository clientRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private InterventionRepository interventionRepository;
  @Autowired private UnavailabilityRepository unavailabilityRepository;
  @Autowired private RecurringUnavailabilityRepository recurringRepository;

  @MockBean private MailGateway mailGateway;

  private String interventionId;

  @BeforeEach
  void setup() {
    recurringRepository.deleteAll();
    unavailabilityRepository.deleteAll();
    interventionRepository.deleteAll();
    resourceRepository.deleteAll();
    clientRepository.deleteAll();
    agencyRepository.deleteAll();

    Agency agency = agencyRepository.save(new Agency("A", "Agence"));
    Client client = clientRepository.save(new Client("C", "Client", "client@example.test"));
    Resource resource = resourceRepository.save(new Resource("R", "Grue", "AB-123-CD", null, agency));
    Intervention intervention =
        interventionRepository.save(
            new Intervention(
                "I",
                "Levage",
                OffsetDateTime.of(2025, 1, 10, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC),
                agency,
                resource,
                client));
    interventionId = intervention.getId();
  }

  @Test
  void pdf_endpoint_returns_pdf() throws Exception {
    byte[] content =
        mvc.perform(get("/api/v1/interventions/" + interventionId + "/pdf"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString("intervention-" + interventionId)))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    assertThat(content).isNotEmpty();
  }

  @Test
  void email_endpoint_returns_accepted_and_sends_mail() throws Exception {
    mvc.perform(
            post("/api/v1/interventions/" + interventionId + "/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"to\":\"client@example.test\"," +
                    "\"subject\":\"Test\"," +
                    "\"message\":\"Bonjour\"}"))
        .andExpect(status().isAccepted());

    ArgumentCaptor<MailGateway.Mail> captor = ArgumentCaptor.forClass(MailGateway.Mail.class);
    verify(mailGateway).send(captor.capture());
    MailGateway.Mail mail = captor.getValue();
    assertThat(mail.to()).isEqualTo("client@example.test");
    assertThat(mail.subject()).isEqualTo("Test");
    assertThat(mail.pdfAttachment()).isNotNull();
    assertThat(mail.pdfAttachment().length).isGreaterThan(0);
  }
}
