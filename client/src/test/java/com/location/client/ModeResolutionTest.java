package com.location.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ModeResolutionTest {

  @Test
  void cliParsing() {
    assertEquals("mock", parse("--datasource=mock"));
    assertEquals("rest", parse("--datasource=rest"));
    assertEquals(null, parse("--datasource=invalid"));
    assertEquals(null, parse("--other=rest"));
  }

  private static String parse(String arg) {
    String[] args = new String[] {arg};
    for (String a : args) {
      if (a.startsWith("--datasource=")) {
        String v = a.substring("--datasource=".length()).trim().toLowerCase();
        if ("mock".equals(v) || "rest".equals(v)) return v;
      }
    }
    return null;
  }
}
