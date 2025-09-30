package com.location.client.ui.uikit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.prefs.Preferences;

/** Utility class to persist frequently used e-mail addresses in user preferences. */
public final class MailFavorites {

  private static final String KEY = "mail.favorites";
  private static final Preferences PREF =
      Preferences.userRoot().node("com.location.locationapp");
  private static final int MAX_FAVORITES = 20;

  private MailFavorites() {}

  public static List<String> get() {
    String raw = PREF.get(KEY, "").trim();
    if (raw.isEmpty()) {
      return List.of();
    }
    Set<String> addresses = new LinkedHashSet<>();
    for (String part : raw.split("\\|")) {
      String email = part == null ? "" : part.trim();
      if (!email.isEmpty()) {
        addresses.add(email);
      }
    }
    if (addresses.isEmpty()) {
      return List.of();
    }
    return Collections.unmodifiableList(new ArrayList<>(addresses));
  }

  public static void add(String email) {
    String normalized = normalize(email);
    if (normalized.isEmpty()) {
      return;
    }
    List<String> current = new ArrayList<>(get());
    current.removeIf(existing -> existing.equalsIgnoreCase(normalized));
    current.add(0, normalized);
    if (current.size() > MAX_FAVORITES) {
      current = new ArrayList<>(current.subList(0, MAX_FAVORITES));
    }
    save(current);
  }

  public static void remove(String email) {
    String normalized = normalize(email);
    if (normalized.isEmpty()) {
      return;
    }
    List<String> current = new ArrayList<>(get());
    if (current.removeIf(existing -> existing.equalsIgnoreCase(normalized))) {
      save(current);
    }
  }

  private static String normalize(String email) {
    if (email == null) {
      return "";
    }
    String trimmed = email.trim();
    if (trimmed.isEmpty()) {
      return "";
    }
    // Normalise domain for duplicate detection but keep original case for display purposes.
    int at = trimmed.indexOf('@');
    if (at >= 0 && at < trimmed.length() - 1) {
      String localPart = trimmed.substring(0, at);
      String domainPart = trimmed.substring(at + 1).toLowerCase(Locale.ROOT);
      return localPart + "@" + domainPart;
    }
    return trimmed;
  }

  private static void save(List<String> addresses) {
    if (addresses.isEmpty()) {
      PREF.remove(KEY);
    } else {
      PREF.put(KEY, String.join("|", addresses));
    }
  }
}
