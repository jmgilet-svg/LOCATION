package com.location.server.api.v1.dto;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Intervention;
import com.location.server.domain.RecurringUnavailability;
import com.location.server.domain.Resource;
import com.location.server.domain.ResourceType;
import com.location.server.domain.Unavailability;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;

public final class ApiV1Dtos {
  private ApiV1Dtos() {}

  public record AgencyDto(String id, String name) {
    public static AgencyDto of(Agency agency) {
      return new AgencyDto(agency.getId(), agency.getName());
    }
  }

  public record ClientDto(String id, String name, String billingEmail) {
    public static ClientDto of(Client client) {
      return new ClientDto(client.getId(), client.getName(), client.getBillingEmail());
    }
  }

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
      String notes) {
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
          intervention.getNotes());
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
      @NotBlank String agencyId,
      @NotBlank String resourceId,
      String driverId,
      @NotBlank String clientId,
      @NotBlank @Size(max = 140) String title,
      @NotNull OffsetDateTime start,
      @NotNull OffsetDateTime end,
      @Size(max = 4000) String notes) {}

  public record UpdateInterventionRequest(
      @NotBlank String agencyId,
      @NotBlank String resourceId,
      String driverId,
      @NotBlank String clientId,
      @NotBlank @Size(max = 140) String title,
      @NotNull OffsetDateTime start,
      @NotNull OffsetDateTime end,
      @Size(max = 4000) String notes) {}

  public record CreateUnavailabilityRequest(
      @NotBlank String resourceId,
      @NotNull OffsetDateTime start,
      @NotNull OffsetDateTime end,
      @NotBlank @Size(max = 140) String reason) {}

  public record CreateRecurringUnavailabilityRequest(
      @NotBlank String resourceId,
      @NotNull DayOfWeek dayOfWeek,
      @NotNull LocalTime start,
      @NotNull LocalTime end,
      @NotBlank @Size(max = 140) String reason) {}
}
