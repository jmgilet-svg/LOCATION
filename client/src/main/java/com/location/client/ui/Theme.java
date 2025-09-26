package com.location.client.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.location.client.core.Preferences;
import java.awt.Window;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Theme {
  public enum Mode { LIGHT, DARK }

  private static Preferences preferences;
  private static Mode current = Mode.LIGHT;
  private static boolean initialized;

  private Theme() {}

  public static void initialize(Preferences prefs) {
    preferences = prefs;
    Mode saved = loadSavedMode();
    applyInternal(saved, false, false);
    initialized = true;
  }

  public static Mode currentMode() {
    return current;
  }

  public static void apply(Mode mode) {
    applyInternal(mode, true, true);
  }

  private static void applyInternal(Mode mode, boolean persist, boolean updateUI) {
    try {
      if (mode == Mode.DARK) {
        UIManager.setLookAndFeel(new FlatDarkLaf());
      } else {
        UIManager.setLookAndFeel(new FlatLightLaf());
      }
      installDefaults();
      current = mode;
      if (persist && preferences != null) {
        preferences.setThemeMode(mode.name());
        preferences.save();
      }
      if (updateUI) {
        FlatLaf.updateUI();
        for (Window window : Window.getWindows()) {
          SwingUtilities.updateComponentTreeUI(window);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static Mode loadSavedMode() {
    if (preferences != null) {
      String saved = preferences.getThemeMode();
      if (saved != null) {
        try {
          return Mode.valueOf(saved);
        } catch (IllegalArgumentException ignored) {
        }
      }
    }
    return Mode.LIGHT;
  }

  public static void ensureInitialized() {
    if (!initialized) {
      applyInternal(Mode.LIGHT, false, false);
      initialized = true;
    }
  }

  public static void installDefaults() {
    UIManager.put("Component.arc", 12);
    UIManager.put("Button.arc", 16);
    UIManager.put("TextComponent.arc", 12);
    UIManager.put("ScrollBar.width", 14);
    UIManager.put("TitlePane.unifiedBackground", true);
  }
}
