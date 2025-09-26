package com.location.server.service;

import com.location.server.domain.Agency;
import com.location.server.domain.CommercialDocument;
import com.location.server.domain.EmailTemplate;
import com.location.server.domain.Intervention;
import com.location.server.repo.AgencyRepository;
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

  public TemplateService(
      EmailTemplateRepository emailTemplateRepository, AgencyRepository agencyRepository) {
    this.emailTemplateRepository = emailTemplateRepository;
    this.agencyRepository = agencyRepository;
  }

  public String renderSubject(String template, Intervention intervention) {
    return render(template, intervention);
  }

  public String renderBody(String template, Intervention intervention) {
    return render(template, intervention);
  }

  public Optional<EmailTemplate> findDocumentTemplate(
      String agencyId, CommercialDocument.DocType docType) {
    return emailTemplateRepository.findByAgencyIdAndDocumentType(agencyId, docType);
  }

  @Transactional
  public EmailTemplate saveDocumentTemplate(
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

  public Map<String, String> documentBindings(CommercialDocument document) {
    Map<String, String> bindings = new HashMap<>();
    bindings.put("agencyName", document.getAgency().getName());
    bindings.put("clientName", document.getClient().getName());
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
