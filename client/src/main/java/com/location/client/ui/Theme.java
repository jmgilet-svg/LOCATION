package com.location.client.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.location.client.core.Preferences;
import com.location.client.ui.uikit.ThemeFonts;
import com.location.client.ui.uikit.ThemePalette;
import java.awt.Font;
import java.awt.Window;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

public final class Theme {
  private static final Logger LOGGER = Logger.getLogger(Theme.class.getName());
  public enum Mode { LIGHT, DARK }

  private static Preferences preferences;
  private static Mode current = Mode.LIGHT;
  private static boolean initialized;
  private static float fontScale = 1.0f;
  private static float lastAppliedScale = 1.0f;
  private static boolean highContrast;

  private Theme() {}

  public static void initialize(Preferences prefs) {
    preferences = prefs;
    fontScale = prefs.getFontScale();
    highContrast = prefs.isHighContrast();
    lastAppliedScale = 1.0f;
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
      applyUiDefaults();
      current = mode;
      if (persist && preferences != null) {
        preferences.setThemeMode(mode.name());
        preferences.save();
      }
      if (updateUI) {
        FlatLaf.updateUI();
        applyUiDefaults();
        for (Window window : Window.getWindows()) {
          SwingUtilities.updateComponentTreeUI(window);
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Impossible d'appliquer le thème", e);
    }
  }

  private static Mode loadSavedMode() {
    if (preferences != null) {
      String saved = preferences.getThemeMode();
      if (saved != null) {
        try {
          return Mode.valueOf(saved);
        } catch (IllegalArgumentException ex) {
          LOGGER.log(Level.WARNING, "Mode de thème inconnu dans les préférences: " + saved, ex);
        }
      }
    }
    return Mode.LIGHT;
  }

  public static void ensureInitialized() {
    if (!initialized) {
      fontScale = 1.0f;
      highContrast = false;
      lastAppliedScale = 1.0f;
      applyInternal(Mode.LIGHT, false, false);
      initialized = true;
    }
  }

  private static void applyUiDefaults() {
    installDefaults();
    ThemePalette.apply("#3D7EFF");
    ThemeFonts.applyDefaultFont();
    applyHighContrastDefaults();
    resetFontDefaults();
  }

  public static void installDefaults() {
    UIManager.put("ScrollBar.width", 14);
    UIManager.put("TitlePane.unifiedBackground", true);
    UIManager.put("TitlePane.menuBarEmbedded", true);
  }

  private static void applyHighContrastDefaults() {
    if (highContrast) {
      UIManager.put("Component.focusWidth", 2);
      UIManager.put("Button.focusedBackground", UIManager.getColor("Component.focusColor"));
    } else {
      UIManager.put("Component.focusWidth", 1);
      UIManager.put("Button.focusedBackground", null);
    }
  }

  private static void resetFontDefaults() {
    lastAppliedScale = 1.0f;
    applyFontScaleDefaults();
  }

  private static void applyFontScaleDefaults() {
    if (Math.abs(fontScale - lastAppliedScale) < 0.001f) {
      return;
    }
    float factor = fontScale / lastAppliedScale;
    UIDefaults defaults = UIManager.getLookAndFeelDefaults();
    Enumeration<Object> keys = defaults.keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = defaults.get(key);
      if (value instanceof Font font) {
        defaults.put(key, font.deriveFont(font.getSize2D() * factor));
      }
    }
    lastAppliedScale = fontScale;
  }

  public static void setHighContrast(boolean value) {
    highContrast = value;
    if (preferences != null) {
      preferences.setHighContrast(value);
      preferences.save();
    }
    applyInternal(current, false, true);
  }

  public static boolean isHighContrast() {
    return highContrast;
  }

  public static void setFontScale(float scale) {
    float clamped = Math.max(0.8f, Math.min(1.6f, scale));
    fontScale = clamped;
    if (preferences != null) {
      preferences.setFontScale(clamped);
      preferences.save();
    }
    applyInternal(current, false, true);
  }

  public static float getFontScale() {
    return fontScale;
  }
}
