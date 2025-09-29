package com.location.server.service;

import com.location.server.domain.Agency;
import com.location.server.domain.CommercialDocument;
import com.location.server.domain.DocumentTemplate;
import com.location.server.domain.EmailTemplate;
import com.location.server.domain.Intervention;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.DocumentTemplateRepository;
import com.location.server.repo.EmailTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final EmailTemplateRepository emailTemplateRepository;
  private final AgencyRepository agencyRepository;
  private final DocumentTemplateRepository documentTemplateRepository;

  public TemplateService(
      EmailTemplateRepository emailTemplateRepository,
      AgencyRepository agencyRepository,
      DocumentTemplateRepository documentTemplateRepository) {
    this.emailTemplateRepository = emailTemplateRepository;
    this.agencyRepository = agencyRepository;
    this.documentTemplateRepository = documentTemplateRepository;
  }

  public String renderSubject(String template, Intervention intervention) {
    return render(template, intervention);
  }

  public String renderBody(String template, Intervention intervention) {
    return render(template, intervention);
  }

  public Optional<EmailTemplate> findDocumentEmailTemplate(
      String agencyId, CommercialDocument.DocType docType) {
    return emailTemplateRepository.findByAgencyIdAndDocumentType(agencyId, docType);
  }

  @Transactional
  public EmailTemplate saveDocumentEmailTemplate(
      String agencyId, CommercialDocument.DocType docType, String subject, String body) {
    Agency agency =
        agencyRepository
            .findById(agencyId)
            .orElseThrow(() -> new EntityNotFoundException("Agency " + agencyId + " not found"));
    EmailTemplate template =
        emailTemplateRepository
            .findByAgencyIdAndDocumentType(agencyId, docType)
            .orElseGet(() -> new EmailTemplate(UUID.randomUUID().toString(), agency, docType, subject, body));
    template.setSubject(subject);
    template.setBody(body);
    return emailTemplateRepository.save(template);
  }

  public Optional<DocumentTemplate> findDocumentTemplate(
      String agencyId, CommercialDocument.DocType docType) {
    return documentTemplateRepository.findByAgencyIdAndDocumentType(agencyId, docType);
  }

  @Transactional
  public DocumentTemplate saveDocumentTemplate(
      String agencyId, CommercialDocument.DocType docType, String html) {
    Agency agency =
        agencyRepository
            .findById(agencyId)
            .orElseThrow(() -> new EntityNotFoundException("Agency " + agencyId + " not found"));
    DocumentTemplate template =
        documentTemplateRepository
            .findByAgencyIdAndDocumentType(agencyId, docType)
            .orElseGet(() -> new DocumentTemplate(UUID.randomUUID().toString(), agency, docType, html));
    template.setHtml(html);
    return documentTemplateRepository.save(template);
  }

  public Map<String, String> documentBindings(CommercialDocument document) {
    Map<String, String> bindings = new HashMap<>();
    bindings.put("agencyName", document.getAgency().getName());
    bindings.put("clientName", document.getClient().getName());
    bindings.put("clientEmail", nullToEmpty(document.getClient().getEmail()));
    bindings.put("clientAddress", nullToEmpty(document.getClient().getAddress()));
    bindings.put("clientZip", nullToEmpty(document.getClient().getZip()));
    bindings.put("clientCity", nullToEmpty(document.getClient().getCity()));
    bindings.put("clientVatNumber", nullToEmpty(document.getClient().getVatNumber()));
    bindings.put("clientIban", nullToEmpty(document.getClient().getIban()));
    bindings.put("docRef", nullToEmpty(document.getReference()));
    bindings.put("docTitle", nullToEmpty(document.getTitle()));
    bindings.put("docType", document.getType().name());
    bindings.put("docDate", document.getDate() == null ? "" : document.getDate().toLocalDate().toString());
    bindings.put("totalTtc", document.getTotalTtc().toPlainString());
    return bindings;
  }

  public String merge(String template, Map<String, String> bindings) {
    if (template == null || template.isBlank()) {
      return "";
    }
    String result = template;
    for (Map.Entry<String, String> entry : bindings.entrySet()) {
      result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return result;
  }

  private String render(String template, Intervention intervention) {
    if (template == null || template.isBlank()) {
      return "";
    }
    Map<String, String> values = new HashMap<>();
    values.put("agencyName", intervention.getAgency().getName());
    values.put("clientName", intervention.getClient().getName());
    values.put("interventionTitle", nullToEmpty(intervention.getTitle()));
    values.put("start", intervention.getStart().format(FORMATTER));
    values.put("end", intervention.getEnd().format(FORMATTER));
    return merge(template, values);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
