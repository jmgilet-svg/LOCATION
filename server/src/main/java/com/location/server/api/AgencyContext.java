package com.location.server.api;

public final class AgencyContext {
  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  private AgencyContext() {}

  public static void set(String agencyId) {
    CURRENT.set(agencyId);
  }

  public static String get() {
    return CURRENT.get();
  }

  public static String require() {
    String agencyId = CURRENT.get();
    if (agencyId == null) {
      throw new IllegalStateException("No agency context bound to current thread");
    }
    return agencyId;
  }

  public static void clear() {
    CURRENT.remove();
  }
}
