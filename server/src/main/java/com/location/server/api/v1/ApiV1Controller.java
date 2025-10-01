package com.location.server.api.v1;

import com.location.server.api.v1.dto.ApiV1Dtos.AgencyDto;
import com.location.server.api.v1.dto.ApiV1Dtos.CreateInterventionRequest;
import com.location.server.api.v1.dto.ApiV1Dtos.CreateRecurringUnavailabilityRequest;
import com.location.server.api.v1.dto.ApiV1Dtos.CreateUnavailabilityRequest;
import com.location.server.api.v1.dto.ApiV1Dtos.InterventionDto;
import com.location.server.api.v1.dto.ApiV1Dtos.ResourceDto;
import com.location.server.api.v1.dto.ApiV1Dtos.RecurringUnavailabilityDto;
import com.location.server.api.v1.dto.ApiV1Dtos.UnavailabilityDto;
import com.location.server.api.v1.dto.ApiV1Dtos.UpdateInterventionRequest;
import com.location.server.api.v1.dto.ApiV1Dtos.SaveAgencyRequest;
import com.location.server.domain.Agency;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import com.location.server.repo.RecurringUnavailabilityRepository;
import com.location.server.repo.UnavailabilityRepository;
import com.location.server.service.InterventionService;
import com.location.server.service.MailGateway;
import com.location.server.service.PdfService;
import com.location.server.service.TemplateService;
import com.location.server.service.UnavailabilityService;
import com.location.server.service.UnavailabilityQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ApiV1Controller {
  private final AgencyRepository agencyRepository;
  private final ClientRepository clientRepository;
  private final ResourceRepository resourceRepository;
  private final InterventionRepository interventionRepository;
  private final InterventionService interventionService;
  private final UnavailabilityRepository unavailabilityRepository;
  private final UnavailabilityService unavailabilityService;
  private final RecurringUnavailabilityRepository recurringUnavailabilityRepository;
  private final UnavailabilityQueryService unavailabilityQueryService;
  private final MailGateway mailGateway;
  private final PdfService pdfService;
  private final TemplateService templateService;

  public ApiV1Controller(
      AgencyRepository agencyRepository,
      ClientRepository clientRepository,
      ResourceRepository resourceRepository,
      InterventionRepository interventionRepository,
      InterventionService interventionService,
      UnavailabilityRepository unavailabilityRepository,
      UnavailabilityService unavailabilityService,
      RecurringUnavailabilityRepository recurringUnavailabilityRepository,
      UnavailabilityQueryService unavailabilityQueryService,
      MailGateway mailGateway,
      PdfService pdfService,
      TemplateService templateService) {
    this.agencyRepository = agencyRepository;
    this.clientRepository = clientRepository;
    this.resourceRepository = resourceRepository;
    this.interventionRepository = interventionRepository;
    this.interventionService = interventionService;
    this.unavailabilityRepository = unavailabilityRepository;
    this.unavailabilityService = unavailabilityService;
    this.recurringUnavailabilityRepository = recurringUnavailabilityRepository;
    this.unavailabilityQueryService = unavailabilityQueryService;
    this.mailGateway = mailGateway;
    this.pdfService = pdfService;
    this.templateService = templateService;
  }

  @GetMapping("/system/features")
  public Map<String, Boolean> features() {
    var env = System.getenv();
    java.util.HashMap<String, Boolean> flags = new java.util.HashMap<>();
    env.forEach(
        (key, value) -> {
          if (key.startsWith("FEATURE_")) {
            boolean enabled =
                "1".equals(value)
                    || "true".equalsIgnoreCase(value)
                    || "yes".equalsIgnoreCase(value)
                    || "on".equalsIgnoreCase(value);
            flags.put(key, enabled);
          }
        });
    flags.putIfAbsent("FEATURE_EMAIL_BULK", true);
    flags.putIfAbsent("FEATURE_RESOURCES_CSV", true);
    flags.putIfAbsent("FEATURE_INTERVENTION_PDF", true);
    flags.putIfAbsent("FEATURE_CLIENTS_CSV", true);
    flags.putIfAbsent("FEATURE_UNAVAILABILITIES_CSV", true);
    return flags;
  }

  @GetMapping("/agencies")
  public List<AgencyDto> agencies() {
    return agencyRepository.findAll().stream().map(AgencyDto::of).collect(Collectors.toList());
  }

  @GetMapping("/agencies/{id}")
  public AgencyDto agency(@PathVariable String id) {
    return agencyRepository
        .findById(id)
        .map(AgencyDto::of)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agence introuvable"));
  }

  @PostMapping("/agencies")
  public AgencyDto saveAgency(@Valid @RequestBody SaveAgencyRequest request) {
    Agency agency;
    if (request.id() != null && !request.id().isBlank()) {
      agency =
          agencyRepository
              .findById(request.id())
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agence introuvable"));
      agency.setName(request.name());
    } else {
      agency = new Agency(UUID.randomUUID().toString(), request.name());
    }
    agencyRepository.save(agency);
    return AgencyDto.of(agency);
  }

  @GetMapping(value = "/clients/csv", produces = "text/csv")
  public ResponseEntity<byte[]> exportClientsCsv() {
    StringBuilder csv =
        new StringBuilder("id;name;email;phone;address;zip;city;vatNumber;iban\n");
    clientRepository
        .findAll()
        .forEach(
            client ->
                csv.append(client.getId())
                    .append(';')
                    .append(sanitize(client.getName()))
                    .append(';')
                    .append(client.getEmail() == null ? "" : client.getEmail())
                    .append(';')
                    .append(client.getPhone() == null ? "" : sanitize(client.getPhone()))
                    .append(';')
                    .append(client.getAddress() == null ? "" : sanitize(client.getAddress()))
                    .append(';')
                    .append(client.getZip() == null ? "" : sanitize(client.getZip()))
                    .append(';')
                    .append(client.getCity() == null ? "" : sanitize(client.getCity()))
                    .append(';')
                    .append(client.getVatNumber() == null ? "" : sanitize(client.getVatNumber()))
                    .append(';')
                    .append(client.getIban() == null ? "" : sanitize(client.getIban()))
                    .append('\n'));
    byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clients.csv\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(bytes);
  }

  @GetMapping("/resources")
  public List<ResourceDto> resources(@RequestParam(required = false) String tags) {
    return resourceRepository.searchByTags(tags).stream().map(ResourceDto::of).collect(Collectors.toList());
  }

  @GetMapping(value = "/resources/csv", produces = "text/csv")
  public ResponseEntity<byte[]> exportResourcesCsv(@RequestParam(required = false) String tags) {
    StringBuilder csv =
        new StringBuilder("id;name;licensePlate;capacityTons;tags;agencyId\n");
    resourceRepository
        .searchByTags(tags)
        .forEach(
            resource ->
                csv.append(resource.getId())
                    .append(';')
                    .append(sanitize(resource.getName()))
                    .append(';')
                    .append(sanitize(resource.getLicensePlate()))
                    .append(';')
                    .append(resource.getCapacityTons() == null ? "" : resource.getCapacityTons())
                    .append(';')
                    .append(resource.getTags() == null ? "" : resource.getTags())
                    .append(';')
                    .append(resource.getAgency().getId())
                    .append('\n'));
    byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resources.csv\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(bytes);
  }

  @GetMapping("/unavailabilities")
  public List<UnavailabilityDto> unavailabilities(
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to,
      @RequestParam(required = false) String resourceId) {
    return unavailabilityQueryService
        .search(from, to, resourceId)
        .stream()
        .map(span -> new UnavailabilityDto(span.id(), span.resourceId(), span.start(), span.end(), span.reason(), span.recurring()))
        .collect(Collectors.toList());
  }

  @GetMapping(value = "/unavailabilities/csv", produces = "text/csv")
  public ResponseEntity<byte[]> exportUnavailabilitiesCsv(
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to,
      @RequestParam(required = false) String resourceId) {
    var spans = unavailabilityQueryService.search(from, to, resourceId);
    StringBuilder csv = new StringBuilder("id;resourceId;start;end;reason;recurring\n");
    spans.forEach(
        span ->
            csv.append(span.id())
                .append(';')
                .append(span.resourceId())
                .append(';')
                .append(span.start())
                .append(';')
                .append(span.end())
                .append(';')
                .append(sanitize(span.reason()))
                .append(';')
                .append(span.recurring())
                .append('\n'));
    byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"unavailabilities.csv\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(bytes);
  }

  @GetMapping("/interventions")
  public List<InterventionDto> interventions(
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to,
      @RequestParam(required = false) String resourceId) {
    return interventionRepository
        .search(from, to, resourceId)
        .stream()
        .map(InterventionDto::of)
        .collect(Collectors.toList());
  }

  @GetMapping(value = "/interventions/csv", produces = "text/csv")
  public ResponseEntity<byte[]> exportCsv(
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to,
      @RequestParam(required = false) String resourceId,
      @RequestParam(required = false) String clientId,
      @RequestParam(required = false) String q) {
    List<InterventionDto> data =
        interventionRepository.search(from, to, resourceId).stream().map(InterventionDto::of).toList();
    Stream<InterventionDto> stream = data.stream();
    if (clientId != null && !clientId.isBlank()) {
      stream = stream.filter(item -> clientId.equals(item.clientId()));
    }
    if (q != null && !q.isBlank()) {
      String query = q.toLowerCase();
      stream =
          stream.filter(
              item -> item.title() != null && item.title().toLowerCase().contains(query));
    }
    StringBuilder csv =
        new StringBuilder("id;title;agencyId;resourceId;clientId;start;end\n");
    stream.forEach(
        item ->
            csv.append(item.id())
                .append(';')
                .append(sanitize(item.title()))
                .append(';')
                .append(item.agencyId())
                .append(';')
                .append(item.resourceId())
                .append(';')
                .append(item.clientId())
                .append(';')
                .append(item.start())
                .append(';')
                .append(item.end())
                .append('\n'));
    byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"interventions.csv\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(bytes);
  }

  private static String sanitize(String value) {
    if (value == null) {
      return "";
    }
    return value.replace(';', ',');
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  @PostMapping("/interventions")
  public InterventionDto create(@Valid @RequestBody CreateInterventionRequest request) {
    return InterventionDto.of(
        interventionService.create(
            request.agencyId(),
            primaryResourceId(request.resourceIds(), request.resourceId()),
            request.driverId(),
            request.clientId(),
            request.title(),
            request.start(),
            request.end(),
            request.notes(),
            request.internalNotes(),
            request.price()));
  }

  @PutMapping("/interventions/{id}")
  public InterventionDto update(
      @PathVariable String id, @Valid @RequestBody UpdateInterventionRequest request) {
    return InterventionDto.of(
        interventionService.update(
            id,
            request.agencyId(),
            primaryResourceId(request.resourceIds(), request.resourceId()),
            request.driverId(),
            request.clientId(),
            request.title(),
            request.start(),
            request.end(),
            request.notes(),
            request.internalNotes(),
            request.price()));
  }

  @DeleteMapping("/interventions/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    interventionService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/unavailabilities")
  public UnavailabilityDto createUnavailability(
      @Valid @RequestBody CreateUnavailabilityRequest request) {
    return UnavailabilityDto.of(
        unavailabilityService.create(
            primaryResourceId(request.resourceIds(), request.resourceId()),
            request.start(),
            request.end(),
            request.reason()));
  }

  @GetMapping("/recurring-unavailabilities")
  public List<RecurringUnavailabilityDto> recurringUnavailabilities(
      @RequestParam(required = false) String resourceId) {
    return recurringUnavailabilityRepository
        .search(resourceId)
        .stream()
        .map(RecurringUnavailabilityDto::of)
        .collect(Collectors.toList());
  }

  @PostMapping("/recurring-unavailabilities")
  public RecurringUnavailabilityDto createRecurring(
      @Valid @RequestBody CreateRecurringUnavailabilityRequest request) {
    return RecurringUnavailabilityDto.of(
        unavailabilityService.createRecurring(
            primaryResourceId(request.resourceIds(), request.resourceId()),
            request.dayOfWeek(),
            request.start(),
            request.end(),
            request.reason()));
  }

  private static String primaryResourceId(
      java.util.List<String> resourceIds, String legacyResourceId) {
    if (resourceIds != null
        && !resourceIds.isEmpty()
        && resourceIds.get(0) != null
        && !resourceIds.get(0).isBlank()) {
      return resourceIds.get(0);
    }
    return legacyResourceId;
  }

  @GetMapping(value = "/interventions/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> exportInterventionPdf(@PathVariable String id) {
    var intervention =
        interventionRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    byte[] pdf = pdfService.buildInterventionPdf(intervention);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"intervention-" + intervention.getId() + ".pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  public record EmailInterventionRequest(@NotBlank @Email String to, String subject, String message) {}

  @PostMapping("/interventions/{id}/email")
  public ResponseEntity<Void> emailIntervention(
      @PathVariable String id, @Valid @RequestBody EmailInterventionRequest request) {
    var intervention =
        interventionRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    byte[] pdf = pdfService.buildInterventionPdf(intervention);
    String subject =
        request.subject() == null || request.subject().isBlank()
            ? "Intervention " + sanitize(intervention.getTitle())
            : request.subject();
    String body =
        request.message() == null || request.message().isBlank()
            ? "Veuillez trouver l'intervention en pièce jointe."
            : request.message();
    mailGateway.send(
        new MailGateway.Mail(
            request.to(), subject, body, pdf, "intervention-" + intervention.getId() + ".pdf"));
    return ResponseEntity.accepted().build();
  }

  public record EmailTemplateDto(String subject, String body) {}

  @GetMapping("/agencies/{id}/email-template")
  public EmailTemplateDto getEmailTemplate(@PathVariable String id) {
    var agency =
        agencyRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return new EmailTemplateDto(agency.getEmailSubjectTemplate(), agency.getEmailBodyTemplate());
  }

  @PutMapping("/agencies/{id}/email-template")
  public EmailTemplateDto updateEmailTemplate(
      @PathVariable String id, @Valid @RequestBody EmailTemplateDto request) {
    var agency =
        agencyRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    agency.setEmailSubjectTemplate(request.subject());
    agency.setEmailBodyTemplate(request.body());
    agencyRepository.save(agency);
    return request;
  }

  public record BulkEmailRequest(List<String> ids, @Email String toOverride) {}

  @PostMapping("/interventions/email-bulk")
  public ResponseEntity<Void> emailBulk(@Valid @RequestBody BulkEmailRequest request) {
    if (request.ids() == null || request.ids().isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    for (String id : request.ids()) {
      var intervention =
          interventionRepository
              .findById(id)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      String recipient =
          request.toOverride() != null && !request.toOverride().isBlank()
              ? request.toOverride()
              : nullToEmpty(intervention.getClient().getEmail());
      if (recipient.isBlank()) {
        continue;
      }
      byte[] pdf = pdfService.buildInterventionPdf(intervention);
      String subjectTemplate = intervention.getAgency().getEmailSubjectTemplate();
      String bodyTemplate = intervention.getAgency().getEmailBodyTemplate();
      String subject =
          subjectTemplate == null || subjectTemplate.isBlank()
              ? "Intervention " + sanitize(intervention.getTitle())
              : templateService.renderSubject(subjectTemplate, intervention);
      String body =
          bodyTemplate == null || bodyTemplate.isBlank()
              ? "Bonjour,\nVeuillez trouver la fiche intervention en pièce jointe."
              : templateService.renderBody(bodyTemplate, intervention);
      mailGateway.send(
          new MailGateway.Mail(
              recipient,
              subject,
              body,
              pdf,
              "intervention-" + intervention.getId() + ".pdf"));
    }
    return ResponseEntity.accepted().build();
  }
}
