package com.location.server.api.v1.dto;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.Intervention;
import com.location.server.domain.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

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
      String id, String name, String licensePlate, Integer colorRgb, AgencyDto agency) {
    public static ResourceDto of(Resource resource) {
      return new ResourceDto(
          resource.getId(),
          resource.getName(),
          resource.getLicensePlate(),
          resource.getColorRgb(),
          AgencyDto.of(resource.getAgency()));
    }
  }

  public record InterventionDto(
      String id,
      String title,
      String agencyId,
      String resourceId,
      String clientId,
      OffsetDateTime start,
      OffsetDateTime end) {
    public static InterventionDto of(Intervention intervention) {
      return new InterventionDto(
          intervention.getId(),
          intervention.getTitle(),
          intervention.getAgency().getId(),
          intervention.getResource().getId(),
          intervention.getClient().getId(),
          intervention.getStart(),
          intervention.getEnd());
    }
  }

  public record CreateInterventionRequest(
      @NotBlank String agencyId,
      @NotBlank String resourceId,
      @NotBlank String clientId,
      @NotBlank @Size(max = 140) String title,
      @NotNull OffsetDateTime start,
      @NotNull OffsetDateTime end) {}
}
