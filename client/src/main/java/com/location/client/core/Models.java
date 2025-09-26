package com.location.client.core;

import java.time.Instant;

public final class Models {
  private Models() {}

  public record Agency(String id, String name) {}

  public record Client(String id, String name, String billingEmail) {}

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
      String resourceId,
      String clientId,
      String driverId,
      String title,
      Instant start,
      Instant end,
      String notes) {}

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

  public record EmailTemplate(String subject, String body) {}

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
}
