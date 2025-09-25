package com.location.client.core;

import java.time.Instant;

public final class Models {
  private Models() {}

  public record Agency(String id, String name) {}

  public record Client(String id, String name, String billingEmail) {}

  public record Intervention(
      String id,
      String agencyId,
      String resourceId,
      String clientId,
      String title,
      Instant start,
      Instant end) {}
}
