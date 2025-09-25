package com.location.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.location.client.core.Preferences;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PreferencesTest {
  @Test
  void store_and_read_filters_ok(@TempDir Path home) {
    String previousHome = System.getProperty("user.home");
    System.setProperty("user.home", home.toString());
    try {
      Preferences prefs = Preferences.load();
      prefs.setFilterQuery("test");
      prefs.setFilterAgencyId("A");
      prefs.setFilterClientId("C");
      prefs.setFilterResourceId("R");
      prefs.setDayIso("2025-01-02");
      prefs.save();

      Preferences reloaded = Preferences.load();
      assertEquals("test", reloaded.getFilterQuery());
      assertEquals("A", reloaded.getFilterAgencyId());
      assertEquals("C", reloaded.getFilterClientId());
      assertEquals("R", reloaded.getFilterResourceId());
      assertEquals("2025-01-02", reloaded.getDayIso());
    } finally {
      if (previousHome != null) {
        System.setProperty("user.home", previousHome);
      } else {
        System.clearProperty("user.home");
      }
    }
  }
}
