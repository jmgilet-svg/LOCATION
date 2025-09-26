package com.location.client.ui.i18n;

import com.location.client.core.Preferences;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Language {
  private static ResourceBundle bundle;
  private static Locale current = Locale.FRENCH;
  private static Preferences preferences;

  private Language() {}

  public static synchronized void initialize(Preferences prefs) {
    preferences = prefs;
    String code = prefs != null ? prefs.getLanguage() : "fr";
    Locale locale = "en".equalsIgnoreCase(code) ? Locale.ENGLISH : Locale.FRENCH;
    loadBundle(locale);
  }

  public static synchronized void setLocale(Locale locale) {
    loadBundle(locale);
    if (preferences != null) {
      preferences.setLanguage(isEnglish() ? "en" : "fr");
      preferences.save();
    }
  }

  private static void loadBundle(Locale locale) {
    current = locale == null ? Locale.FRENCH : locale;
    bundle = ResourceBundle.getBundle("messages", current);
  }

  public static synchronized boolean isEnglish() {
    return Locale.ENGLISH.getLanguage().equals(current.getLanguage());
  }

  public static synchronized String tr(String key) {
    if (bundle == null) {
      loadBundle(current);
    }
    try {
      return bundle.getString(key);
    } catch (MissingResourceException ex) {
      return key;
    }
  }
}
