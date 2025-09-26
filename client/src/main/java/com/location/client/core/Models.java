package com.location.client.core;

import java.time.Instant;

public final class Models {
  private Models() {}

  public record Agency(String id, String name) {}

  public record Client(String id, String name, String billingEmail) {}

  public record Resource(String id, String name, String licensePlate, Integer colorRgb, String agencyId) {}

  public record Intervention(
      String id,
      String agencyId,
      String resourceId,
      String clientId,
      String title,
      Instant start,
      Instant end) {}

  public record Unavailability(
      String id, String resourceId, String reason, Instant start, Instant end) {}
}
