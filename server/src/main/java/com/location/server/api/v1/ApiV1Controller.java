package com.location.server.api.v1;

import com.location.server.api.v1.dto.ApiV1Dtos.AgencyDto;
import com.location.server.api.v1.dto.ApiV1Dtos.ClientDto;
import com.location.server.api.v1.dto.ApiV1Dtos.CreateInterventionRequest;
import com.location.server.api.v1.dto.ApiV1Dtos.CreateRecurringUnavailabilityRequest;
import com.location.server.api.v1.dto.ApiV1Dtos.CreateUnavailabilityRequest;
import com.location.server.api.v1.dto.ApiV1Dtos.InterventionDto;
import com.location.server.api.v1.dto.ApiV1Dtos.ResourceDto;
import com.location.server.api.v1.dto.ApiV1Dtos.RecurringUnavailabilityDto;
import com.location.server.api.v1.dto.ApiV1Dtos.UnavailabilityDto;
import com.location.server.api.v1.dto.ApiV1Dtos.UpdateInterventionRequest;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import com.location.server.repo.RecurringUnavailabilityRepository;
import com.location.server.repo.UnavailabilityRepository;
import com.location.server.service.InterventionService;
import com.location.server.service.MailGateway;
import com.location.server.service.PdfService;
import com.location.server.service.UnavailabilityService;
import com.location.server.service.UnavailabilityQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.nio.charset.StandardCharsets;
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
      PdfService pdfService) {
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
  }

  @GetMapping("/agencies")
  public List<AgencyDto> agencies() {
    return agencyRepository.findAll().stream().map(AgencyDto::of).collect(Collectors.toList());
  }

  @GetMapping("/clients")
  public List<ClientDto> clients() {
    return clientRepository.findAll().stream().map(ClientDto::of).collect(Collectors.toList());
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

  @PostMapping("/interventions")
  public InterventionDto create(@Valid @RequestBody CreateInterventionRequest request) {
    return InterventionDto.of(
        interventionService.create(
            request.agencyId(),
            request.resourceId(),
            request.clientId(),
            request.title(),
            request.start(),
            request.end()));
  }

  @PutMapping("/interventions/{id}")
  public InterventionDto update(
      @PathVariable String id, @Valid @RequestBody UpdateInterventionRequest request) {
    return InterventionDto.of(
        interventionService.update(
            id,
            request.agencyId(),
            request.resourceId(),
            request.clientId(),
            request.title(),
            request.start(),
            request.end()));
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
            request.resourceId(), request.start(), request.end(), request.reason()));
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
            request.resourceId(),
            request.dayOfWeek(),
            request.start(),
            request.end(),
            request.reason()));
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
}
