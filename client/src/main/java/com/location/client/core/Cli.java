package com.location.client.core;

public final class Cli {
  private Cli() {}

  public static String parseDataSourceArg(String[] args) {
    if (args == null) return null;
    for (String arg : args) {
      if (arg == null) continue;
      if (arg.startsWith("--datasource=")) {
        String value = arg.substring("--datasource=".length()).trim().toLowerCase();
        return switch (value) {
          case "mock", "rest" -> value;
          default -> null;
        };
      }
    }
    return null;
  }
}
