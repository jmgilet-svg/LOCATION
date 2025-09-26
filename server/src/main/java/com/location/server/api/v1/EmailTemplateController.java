package com.location.server.api.v1;

import com.location.server.api.AgencyContext;
import com.location.server.domain.CommercialDocument;
import com.location.server.domain.EmailTemplate;
import com.location.server.service.TemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
public class EmailTemplateController {

  private final TemplateService templateService;

  public EmailTemplateController(TemplateService templateService) {
    this.templateService = templateService;
  }

  @GetMapping("/{docType}")
  public ResponseEntity<TemplateDto> get(@PathVariable CommercialDocument.DocType docType) {
    String agencyId = AgencyContext.require();
    return templateService
        .findDocumentTemplate(agencyId, docType)
        .map(template -> ResponseEntity.ok(toDto(template)))
        .orElse(ResponseEntity.noContent().build());
  }

  @PutMapping("/{docType}")
  public TemplateDto put(
      @PathVariable CommercialDocument.DocType docType, @Valid @RequestBody SaveRequest request) {
    String agencyId = AgencyContext.require();
    EmailTemplate template =
        templateService.saveDocumentTemplate(agencyId, docType, request.subject(), request.body());
    return toDto(template);
  }

  private static TemplateDto toDto(EmailTemplate template) {
    return new TemplateDto(
        template.getAgency().getId(),
        template.getDocumentType(),
        template.getSubject(),
        template.getBody());
  }

  public record TemplateDto(
      String agencyId, CommercialDocument.DocType docType, String subject, String body) {}

  public record SaveRequest(@NotBlank String subject, @NotBlank String body) {}
}
