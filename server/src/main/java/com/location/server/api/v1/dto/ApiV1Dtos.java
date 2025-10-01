package com.location.server.api.v1.dto;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.ClientContact;
import com.location.server.domain.Intervention;
import com.location.server.domain.RecurringUnavailability;
import com.location.server.domain.Resource;
import com.location.server.domain.ResourceType;
import com.location.server.domain.Unavailability;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Locale;

public final class ApiV1Dtos {
  private ApiV1Dtos() {}

  public record AgencyDto(String id, String name, String legalFooter, String iban, String logoDataUri) {
    public static AgencyDto of(Agency agency) {
      return new AgencyDto(agency.getId(), agency.getName(), null, null, null);
    }
  }

  public record SaveAgencyRequest(
      String id,
      @NotBlank @Size(max = 128) String name,
      String legalFooter,
      String iban,
      String logoDataUri) {}

  public record ClientDto(
      String id,
      String name,
      String email,
      String phone,
      String address,
      String zip,
      String city,
      String vatNumber,
      String iban) {
    public static ClientDto of(Client client) {
      return new ClientDto(
          client.getId(),
          client.getName(),
          client.getEmail(),
          client.getPhone(),
          client.getAddress(),
          client.getZip(),
          client.getCity(),
          client.getVatNumber(),
          client.getIban());
    }
  }

  public record SaveClientRequest(
      String id,
      @NotBlank @Size(max = 128) String name,
      @Size(max = 160) String email,
      @Size(max = 50) String phone,
      @Size(max = 200) String address,
      @Size(max = 16) String zip,
      @Size(max = 120) String city,
      @Size(max = 32) String vatNumber,
      @Size(max = 34) String iban) {}

  public record ContactDto(
      String id,
      String clientId,
      String firstName,
      String lastName,
      String email,
      String phone) {
    public static ContactDto of(ClientContact contact) {
      return new ContactDto(
          contact.getId(),
          contact.getClient().getId(),
          contact.getFirstName(),
          contact.getLastName(),
          contact.getEmail(),
          contact.getPhone());
    }
  }

  public record SaveContactRequest(
      String id,
      @NotBlank String clientId,
      @Size(max = 60) String firstName,
      @Size(max = 60) String lastName,
      @Size(max = 200) String email,
      @Size(max = 50) String phone) {}

  public record ResourceDto(
      String id,
      String name,
      String licensePlate,
      Integer colorRgb,
      AgencyDto agency,
      String tags,
      Integer capacityTons,
      String resourceTypeId) {
    public static ResourceDto of(Resource resource) {
      return new ResourceDto(
          resource.getId(),
          resource.getName(),
          resource.getLicensePlate(),
          resource.getColorRgb(),
          AgencyDto.of(resource.getAgency()),
          resource.getTags(),
          resource.getCapacityTons(),
          resource.getResourceType() == null ? null : resource.getResourceType().getId());
    }
  }

  public record ResourceTypeDto(String id, @NotBlank String name, @NotBlank String iconName) {
    public static ResourceTypeDto of(ResourceType type) {
      return new ResourceTypeDto(type.getId(), type.getName(), type.getIconName());
    }
  }

  public record ResourceTypeAssignmentDto(String resourceTypeId) {}

  public record ResourceTypeAssignmentRequest(@NotBlank String resourceTypeId) {}

  public record InterventionDto(
      String id,
      String title,
      String agencyId,
      String resourceId,
      String driverId,
      String clientId,
      OffsetDateTime start,
      OffsetDateTime end,
      String notes,
      String internalNotes,
      Double price,
      java.util.List<String> resourceIds) {
    public static InterventionDto of(Intervention intervention) {
      return new InterventionDto(
          intervention.getId(),
          intervention.getTitle(),
          intervention.getAgency().getId(),
          intervention.getResource().getId(),
          intervention.getDriver() == null ? null : intervention.getDriver().getId(),
          intervention.getClient().getId(),
          intervention.getStart(),
          intervention.getEnd(),
          intervention.getNotes(),
          intervention.getInternalNotes(),
          intervention.getPrice(),
          java.util.List.of(intervention.getResource().getId()));
    }
  }

  public record UnavailabilityDto(
      String id,
      String resourceId,
      OffsetDateTime start,
      OffsetDateTime end,
      String reason,
      boolean recurring) {
    public static UnavailabilityDto of(Unavailability unavailability) {
      return new UnavailabilityDto(
          unavailability.getId(),
          unavailability.getResource().getId(),
          unavailability.getStart(),
          unavailability.getEnd(),
          unavailability.getReason(),
          false);
    }
  }

  public record RecurringUnavailabilityDto(
      String id,
      String resourceId,
      DayOfWeek dayOfWeek,
      LocalTime start,
      LocalTime end,
      String reason) {
    public static RecurringUnavailabilityDto of(RecurringUnavailability recurring) {
      return new RecurringUnavailabilityDto(
          recurring.getId(),
          recurring.getResource().getId(),
          recurring.getDayOfWeek(),
          recurring.getStartTime(),
          recurring.getEndTime(),
          recurring.getReason());
    }
  }

  public record CreateInterventionRequest(
      java.util.List<String> resourceIds,
      @NotBlank String agencyId,
      @NotBlank String resourceId,
      String driverId,
      @NotBlank String clientId,
      @NotBlank @Size(max = 140) String title,
      @NotNull OffsetDateTime start,
      @NotNull OffsetDateTime end,
      @Size(max = 4000) String notes,
      @Size(max = 4000) String internalNotes,
      @DecimalMin(value = "0.0", inclusive = true) Double price) {}

  public record UpdateInterventionRequest(
      java.util.List<String> resourceIds,
      @NotBlank String agencyId,
      @NotBlank String resourceId,
      String driverId,
      @NotBlank String clientId,
      @NotBlank @Size(max = 140) String title,
      @NotNull OffsetDateTime start,
      @NotNull OffsetDateTime end,
      @Size(max = 4000) String notes,
      @Size(max = 4000) String internalNotes,
      @DecimalMin(value = "0.0", inclusive = true) Double price) {}

  public record CreateUnavailabilityRequest(
      java.util.List<String> resourceIds,
      @NotBlank String resourceId,
      @NotNull OffsetDateTime start,
      @NotNull OffsetDateTime end,
      @NotBlank @Size(max = 140) String reason) {}

  public record CreateRecurringUnavailabilityRequest(
      java.util.List<String> resourceIds,
      @NotBlank String resourceId,
      @NotNull DayOfWeek dayOfWeek,
      @NotNull LocalTime start,
      @NotNull LocalTime end,
      @NotBlank @Size(max = 140) String reason) {}
}
