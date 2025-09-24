package com.location.client.core;

public final class Models {
  private Models() {}

  public record Agency(String id, String name) {}

  public record Client(String id, String name, String billingEmail) {}
}
