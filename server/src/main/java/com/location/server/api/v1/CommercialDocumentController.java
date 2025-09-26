package com.location.server.api.v1;

import com.location.server.api.AgencyContext;
import com.location.server.domain.CommercialDocument;
import com.location.server.domain.CommercialDocument.DocType;
import com.location.server.domain.CommercialDocumentLine;
import com.location.server.repo.CommercialDocumentRepository;
import com.location.server.service.CommercialDocumentPdfService;
import com.location.server.service.CommercialDocumentService;
import com.location.server.service.MailGateway;
import com.location.server.service.TemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/docs")
public class CommercialDocumentController {
  private final CommercialDocumentRepository documentRepository;
  private final CommercialDocumentService documentService;
  private final CommercialDocumentPdfService pdfService;
  private final MailGateway mailGateway;
  private final TemplateService templateService;

  public CommercialDocumentController(
      CommercialDocumentRepository documentRepository,
      CommercialDocumentService documentService,
      CommercialDocumentPdfService pdfService,
      MailGateway mailGateway,
      TemplateService templateService) {
    this.documentRepository = documentRepository;
    this.documentService = documentService;
    this.pdfService = pdfService;
    this.mailGateway = mailGateway;
    this.templateService = templateService;
  }

  @GetMapping
  public List<DocDto> list(
      @RequestParam(required = false) DocType type,
      @RequestParam(required = false) String clientId,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    String agencyId = AgencyContext.require();
    return documentRepository.search(agencyId, type, clientId, from, to).stream()
        .map(CommercialDocumentController::toDto)
        .collect(Collectors.toList());
  }

  @GetMapping(value = ".csv", produces = "text/csv; charset=UTF-8")
  public ResponseEntity<String> exportCsv(
      @RequestParam(required = false) DocType type,
      @RequestParam(required = false) String clientId,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to) {
    String agencyId = AgencyContext.require();
    List<CommercialDocument> documents = documentRepository.search(agencyId, type, clientId, from, to);
    StringBuilder csv = new StringBuilder("type;reference;title;client;date;ht;tva;ttc\n");
    for (CommercialDocument document : documents) {
      csv.append(document.getType())
          .append(';')
          .append(sanitize(document.getReference()))
          .append(';')
          .append(sanitize(document.getTitle()))
          .append(';')
          .append(sanitize(document.getClient().getName()))
          .append(';')
          .append(document.getDate() == null ? "" : document.getDate().toLocalDate())
          .append(';')
          .append(document.getTotalHt().toPlainString())
          .append(';')
          .append(document.getTotalVat().toPlainString())
          .append(';')
          .append(document.getTotalTtc().toPlainString())
          .append('\n');
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documents.csv\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csv.toString());
  }

  @PostMapping
  public DocDto create(@Valid @RequestBody CreateDocRequest request) {
    CommercialDocument document =
        documentService.create(request.type(), request.agencyId(), request.clientId(), request.title());
    return toDto(document);
  }

  @GetMapping("/{id}")
  public DocDto get(@PathVariable String id) {
    CommercialDocument document = documentRepository.findById(id).orElseThrow();
    return toDto(document);
  }

  @PutMapping("/{id}")
  public DocDto update(@PathVariable String id, @Valid @RequestBody UpdateDocRequest request) {
    List<CommercialDocumentService.LinePayload> lines =
        request.lines() == null
            ? List.of()
            : request.lines().stream()
                .map(line -> new CommercialDocumentService.LinePayload(
                    line.designation(), line.quantity(), line.unitPrice(), line.vatRate()))
                .collect(Collectors.toList());
    CommercialDocument document = documentService.update(id, request.reference(), request.title(), lines);
    return toDto(document);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable String id) {
    documentRepository.deleteById(id);
  }

  @PostMapping("/{id}/transition")
  public DocDto transition(@PathVariable String id, @RequestParam("to") DocType toType) {
    CommercialDocument document = documentService.transition(id, toType);
    return toDto(document);
  }

  @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> pdf(@PathVariable String id) {
    CommercialDocument document = documentRepository.findById(id).orElseThrow();
    byte[] bytes = pdfService.build(document);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + document.getType().name().toLowerCase() + "-" + document.getId() + ".pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(bytes);
  }

  @PostMapping("/{id}/email")
  public ResponseEntity<Void> email(@PathVariable String id, @Valid @RequestBody EmailRequest request) {
    CommercialDocument document = documentRepository.findById(id).orElseThrow();
    MailGateway.Mail mail = buildMail(request.to, request.subject, request.message, document);
    mailGateway.send(mail);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/email-batch")
  public ResponseEntity<Void> emailBatch(@Valid @RequestBody EmailBatchRequest request) {
    for (String documentId : request.ids()) {
      CommercialDocument document = documentRepository.findById(documentId).orElseThrow();
      MailGateway.Mail mail =
          buildMail(request.to(), request.subject(), request.message(), document);
      mailGateway.send(mail);
    }
    return ResponseEntity.accepted().build();
  }

  private MailGateway.Mail buildMail(
      String to, String subject, String message, CommercialDocument document) {
    byte[] pdf = pdfService.build(document);
    String effectiveSubject = subject;
    String effectiveBody = message;
    if ((effectiveSubject == null || effectiveSubject.isBlank())
        || (effectiveBody == null || effectiveBody.isBlank())) {
      var optionalTemplate =
          templateService.findDocumentEmailTemplate(
              document.getAgency().getId(), document.getType());
      if (optionalTemplate.isPresent()) {
        var template = optionalTemplate.get();
        var bindings = templateService.documentBindings(document);
        if (effectiveSubject == null || effectiveSubject.isBlank()) {
          effectiveSubject = templateService.merge(template.getSubject(), bindings);
        }
        if (effectiveBody == null || effectiveBody.isBlank()) {
          effectiveBody = templateService.merge(template.getBody(), bindings);
        }
      }
    }
    if (effectiveSubject == null || effectiveSubject.isBlank()) {
      effectiveSubject = titleFor(document);
    }
    if (effectiveBody == null || effectiveBody.isBlank()) {
      effectiveBody = "Bonjour,\nVeuillez trouver le document en pièce jointe.";
    }
    return new MailGateway.Mail(to, effectiveSubject, effectiveBody, pdf, filenameFor(document));
  }

  private static DocDto toDto(CommercialDocument document) {
    List<DocLineDto> lines = document.getLines().stream().map(CommercialDocumentController::toDto).toList();
    return new DocDto(
        document.getId(),
        document.getType().name(),
        document.getStatus().name(),
        document.getReference(),
        document.getTitle(),
        document.getAgency().getId(),
        document.getClient().getId(),
        document.getDate(),
        document.getTotalHt().doubleValue(),
        document.getTotalVat().doubleValue(),
        document.getTotalTtc().doubleValue(),
        lines);
  }

  private static DocLineDto toDto(CommercialDocumentLine line) {
    return new DocLineDto(
        line.getDesignation(),
        line.getQuantity().doubleValue(),
        line.getUnitPrice().doubleValue(),
        line.getVatRate().doubleValue());
  }

  private static String titleFor(CommercialDocument document) {
    String base =
        switch (document.getType()) {
          case QUOTE -> "Devis";
          case ORDER -> "Commande";
          case DELIVERY -> "Bon de livraison";
          case INVOICE -> "Facture";
        };
    return document.getReference() == null || document.getReference().isBlank()
        ? base
        : base + " " + document.getReference();
  }

  private static String filenameFor(CommercialDocument document) {
    return document.getType().name().toLowerCase() + "-" + document.getId() + ".pdf";
  }

  private static String sanitize(String value) {
    if (value == null) {
      return "";
    }
    return value.replace(';', ',').replace('\n', ' ').replace('\r', ' ');
  }

  public record DocLineDto(String designation, double quantity, double unitPrice, double vatRate) {}

  public record DocDto(
      String id,
      String type,
      String status,
      String reference,
      String title,
      String agencyId,
      String clientId,
      OffsetDateTime date,
      double totalHt,
      double totalVat,
      double totalTtc,
      List<DocLineDto> lines) {}

  public record CreateDocRequest(
      @NotNull DocType type, @NotBlank String agencyId, @NotBlank String clientId, @NotBlank String title) {}

  public record UpdateDocRequest(String reference, @NotBlank String title, List<@Valid DocLineDto> lines) {}

  public record EmailRequest(@NotBlank @Email String to, String subject, String message) {}

  public record EmailBatchRequest(
      @NotEmpty List<String> ids, @NotBlank @Email String to, String subject, String message) {}
}
