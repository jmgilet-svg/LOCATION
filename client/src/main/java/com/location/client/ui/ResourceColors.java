package com.location.client.ui;

import com.location.client.core.Models;
import com.location.client.core.Preferences;
import java.awt.Color;

public final class ResourceColors {
  private static final Color DEFAULT_COLOR = new Color(66, 133, 244);
  private static Preferences preferences;

  private ResourceColors() {}

  public static void initialize(Preferences prefs) {
    preferences = prefs;
  }

  public static Color colorFor(Models.Resource resource) {
    if (resource == null) {
      return DEFAULT_COLOR;
    }
    Color override = parseHex(getOverrideHex(resource.id()));
    if (override != null) {
      return override;
    }
    Integer rgb = resource.colorRgb();
    if (rgb != null) {
      try {
        return new Color(rgb, false);
      } catch (IllegalArgumentException ignored) {
      }
    }
    return DEFAULT_COLOR;
  }

  public static void setOverride(String resourceId, Color color) {
    if (preferences == null || resourceId == null || resourceId.isBlank()) {
      return;
    }
    if (color == null) {
      preferences.setResourceColor(resourceId, null);
    } else {
      preferences.setResourceColor(resourceId, toHex(color));
    }
    preferences.save();
  }

  public static String getOverrideHex(String resourceId) {
    if (preferences == null || resourceId == null || resourceId.isBlank()) {
      return null;
    }
    return preferences.getResourceColor(resourceId);
  }

  public static void clearOverride(String resourceId) {
    setOverride(resourceId, null);
  }

  private static Color parseHex(String hex) {
    if (hex == null || hex.isBlank()) {
      return null;
    }
    String clean = hex.trim();
    if (clean.startsWith("#")) {
      clean = clean.substring(1);
    }
    if (clean.length() != 6) {
      return null;
    }
    try {
      int rgb = Integer.parseInt(clean, 16);
      return new Color(rgb);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static String toHex(Color color) {
    return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
  }
}
