package com.location.server.api.v1;

import com.location.server.api.AgencyContext;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.InterventionRepository;
import com.location.server.repo.ResourceRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ExportController {
  private final ClientRepository clientRepository;
  private final ResourceRepository resourceRepository;
  private final InterventionRepository interventionRepository;

  public ExportController(
      ClientRepository clientRepository,
      ResourceRepository resourceRepository,
      InterventionRepository interventionRepository) {
    this.clientRepository = clientRepository;
    this.resourceRepository = resourceRepository;
    this.interventionRepository = interventionRepository;
  }

  @GetMapping(value = "/clients.csv", produces = "text/csv; charset=UTF-8")
  public ResponseEntity<byte[]> exportClientsCsv() {
    AgencyContext.require();
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
                    .append(client.getEmail() == null ? "" : sanitize(client.getEmail()))
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
    return csvResponse("clients.csv", csv);
  }

  @GetMapping(value = "/resources.csv", produces = "text/csv; charset=UTF-8")
  public ResponseEntity<byte[]> exportResourcesCsv() {
    String agencyId = AgencyContext.require();
    StringBuilder csv =
        new StringBuilder("id;name;licensePlate;capacityTons;tags;agencyId\n");
    resourceRepository
        .findAll()
        .stream()
        .filter(resource -> resource.getAgency() != null)
        .filter(resource -> agencyId.equals(resource.getAgency().getId()))
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
                    .append(resource.getTags() == null ? "" : sanitize(resource.getTags()))
                    .append(';')
                    .append(resource.getAgency().getId())
                    .append('\n'));
    return csvResponse("resources.csv", csv);
  }

  @GetMapping(value = "/interventions.csv", produces = "text/csv; charset=UTF-8")
  public ResponseEntity<byte[]> exportInterventionsCsv(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
    String agencyId = AgencyContext.require();
    if (from.isAfter(to)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
    }
    StringBuilder csv =
        new StringBuilder("id;title;clientId;resourceId;driverId;start;end\n");
    interventionRepository
        .search(from, to, null)
        .stream()
        .filter(intervention -> intervention.getAgency() != null)
        .filter(intervention -> agencyId.equals(intervention.getAgency().getId()))
        .forEach(
            intervention ->
                csv.append(intervention.getId())
                    .append(';')
                    .append(sanitize(intervention.getTitle()))
                    .append(';')
                    .append(intervention.getClient().getId())
                    .append(';')
                    .append(intervention.getResource().getId())
                    .append(';')
                    .append(intervention.getDriver() == null ? "" : intervention.getDriver().getId())
                    .append(';')
                    .append(intervention.getStart())
                    .append(';')
                    .append(intervention.getEnd())
                    .append('\n'));
    return csvResponse("interventions.csv", csv);
  }

  private static ResponseEntity<byte[]> csvResponse(String filename, StringBuilder content) {
    byte[] bytes = content.toString().getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(bytes);
  }

  private static String sanitize(String value) {
    if (value == null) {
      return "";
    }
    return value.replace(';', ',');
  }
}
