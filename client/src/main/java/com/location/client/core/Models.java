package com.location.client.core;

import java.time.Instant;
import java.util.List;

public final class Models {
  private Models() {}

  public record Agency(String id, String name, String legalFooter, String iban, String logoDataUri) {
    public Agency(String id, String name) {
      this(id, name, null, null, null);
    }
  }

  public record Client(
      String id,
      String name,
      String billingEmail,
      String billingAddress,
      String billingZip,
      String billingCity,
      String vatNumber,
      String iban) {}

  public record Driver(String id, String name, String email) {}

  public record Resource(
      String id,
      String name,
      String licensePlate,
      Integer colorRgb,
      String agencyId,
      String tags,
      Integer capacityTons) {
    @Override
    public String toString() {
      return capacityTons == null ? name : name + " (" + capacityTons + "t)";
    }
  }

  public record Intervention(
      String id,
      String agencyId,
      List<String> resourceIds,
      String clientId,
      String driverId,
      String title,
      Instant start,
      Instant end,
      String notes,
      Double price) {
    public Intervention {
      resourceIds = resourceIds == null ? List.of() : List.copyOf(resourceIds);
    }

    public Intervention(
        String id,
        String agencyId,
        String resourceId,
        String clientId,
        String driverId,
        String title,
        Instant start,
        Instant end,
        String notes) {
      this(id, agencyId, resourceId, clientId, driverId, title, start, end, notes, null);
    }

    public Intervention(
        String id,
        String agencyId,
        List<String> resourceIds,
        String clientId,
        String driverId,
        String title,
        Instant start,
        Instant end,
        String notes) {
      this(id, agencyId, resourceIds, clientId, driverId, title, start, end, notes, null);
    }

    public Intervention(
        String id,
        String agencyId,
        String resourceId,
        String clientId,
        String driverId,
        String title,
        Instant start,
        Instant end,
        String notes,
        Double price) {
      this(
          id,
          agencyId,
          resourceId == null || resourceId.isBlank() ? List.of() : List.of(resourceId),
          clientId,
          driverId,
          title,
          start,
          end,
          notes,
          price);
    }

    public String resourceId() {
      return resourceIds.isEmpty() ? null : resourceIds.get(0);
    }

    public Intervention withResourceIds(List<String> newResourceIds) {
      return new Intervention(
          id,
          agencyId,
          newResourceIds == null ? List.of() : List.copyOf(newResourceIds),
          clientId,
          driverId,
          title,
          start,
          end,
          notes,
          price);
    }
  }

  public record Unavailability(
      String id,
      String resourceId,
      String reason,
      Instant start,
      Instant end,
      boolean recurring) {}

  public record RecurringUnavailability(
      String id,
      String resourceId,
      java.time.DayOfWeek dayOfWeek,
      java.time.LocalTime start,
      java.time.LocalTime end,
      String reason) {}

  public record EmailTemplate(String key, String subject, String html) {
    public EmailTemplate(String subject, String html) {
      this(null, subject, html);
    }

    public String body() {
      return html;
    }
  }
  public record DocTemplate(String html) {}

  public record DocLine(String designation, double quantity, double unitPrice, double vatRate) {}

  public record Doc(
      String id,
      String type,
      String status,
      String reference,
      String title,
      String agencyId,
      String clientId,
      java.time.OffsetDateTime date,
      double totalHt,
      double totalVat,
      double totalTtc,
      java.util.List<DocLine> lines) {}

  public record ResourceType(String id, String name, String iconName) {}
}
