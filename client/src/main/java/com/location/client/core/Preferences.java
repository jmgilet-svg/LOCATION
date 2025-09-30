package com.location.client.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Preferences {
  private final Properties props = new Properties();
  private final Path file;

  private Preferences(Path file) {
    this.file = file;
  }

  public static Preferences load() {
    Path home = Path.of(System.getProperty("user.home"), ".location");
    try {
      Files.createDirectories(home);
    } catch (IOException ignored) {
    }
    Path file = home.resolve("app.properties");
    Preferences prefs = new Preferences(file);
    if (Files.exists(file)) {
      try (var in = Files.newInputStream(file)) {
        prefs.props.load(in);
      } catch (IOException ignored) {
      }
    }
    prefs.props.putIfAbsent("baseUrl", "http://localhost:8080");
    prefs.props.putIfAbsent("restUser", "demo");
    prefs.props.putIfAbsent("restPass", "demo");
    return prefs;
  }

  public void save() {
    try (var out = Files.newOutputStream(file)) {
      props.store(out, "LOCATION");
    } catch (IOException ignored) {
    }
  }

  public String getLastSource() {
    return props.getProperty("lastSource");
  }

  public void setLastSource(String value) {
    props.setProperty("lastSource", value);
  }

  public String getThemeMode() {
    return props.getProperty("theme", "LIGHT");
  }

  public void setThemeMode(String value) {
    if (value == null || value.isBlank()) {
      props.remove("theme");
    } else {
      props.setProperty("theme", value);
    }
  }

  public float getFontScale() {
    String raw = props.getProperty("fontScale", "1.0");
    try {
      return Float.parseFloat(raw);
    } catch (NumberFormatException ex) {
      return 1.0f;
    }
  }

  public void setFontScale(float value) {
    props.setProperty("fontScale", Float.toString(value));
  }

  public boolean isHighContrast() {
    return Boolean.parseBoolean(props.getProperty("highContrast", "false"));
  }

  public void setHighContrast(boolean value) {
    props.setProperty("highContrast", Boolean.toString(value));
  }

  public String getLanguage() {
    return props.getProperty("lang", "fr");
  }

  public void setLanguage(String code) {
    if (code == null || code.isBlank()) {
      props.remove("lang");
    } else {
      props.setProperty("lang", code);
    }
  }

  public String getBaseUrl() {
    return props.getProperty("baseUrl");
  }

  public void setBaseUrl(String value) {
    props.setProperty("baseUrl", value);
  }

  public String getRestUser() {
    return props.getProperty("restUser", "demo");
  }

  public void setRestUser(String value) {
    props.setProperty("restUser", value);
  }

  public String getRestPass() {
    return props.getProperty("restPass", "demo");
  }

  public void setRestPass(String value) {
    props.setProperty("restPass", value);
  }

  public String getLastEmailTo() {
    return props.getProperty("lastEmailTo", "");
  }

  public void setLastEmailTo(String value) {
    props.setProperty("lastEmailTo", value == null ? "" : value);
  }

  public String getCurrentAgencyId() {
    return props.getProperty("currentAgencyId");
  }

  public void setCurrentAgencyId(String value) {
    if (value == null || value.isBlank()) {
      props.remove("currentAgencyId");
    } else {
      props.setProperty("currentAgencyId", value);
    }
  }

  public String getFilterAgencyId() {
    return props.getProperty("filterAgencyId");
  }

  public void setFilterAgencyId(String value) {
    if (value == null) {
      props.remove("filterAgencyId");
    } else {
      props.setProperty("filterAgencyId", value);
    }
  }

  public String getFilterResourceId() {
    return props.getProperty("filterResourceId");
  }

  public void setFilterResourceId(String value) {
    if (value == null) {
      props.remove("filterResourceId");
    } else {
      props.setProperty("filterResourceId", value);
    }
  }

  public String getFilterClientId() {
    return props.getProperty("filterClientId");
  }

  public void setFilterClientId(String value) {
    if (value == null) {
      props.remove("filterClientId");
    } else {
      props.setProperty("filterClientId", value);
    }
  }

  public String getFilterQuery() {
    return props.getProperty("filterQuery", "");
  }

  public void setFilterQuery(String value) {
    props.setProperty("filterQuery", value == null ? "" : value);
  }

  public String getFilterTags() {
    return props.getProperty("filterTags", "");
  }

  public void setFilterTags(String value) {
    props.setProperty("filterTags", value == null ? "" : value);
  }

  public String getInterventionTagFilter() {
    return props.getProperty("filterInterventionTags", "");
  }

  public void setInterventionTagFilter(String value) {
    props.setProperty("filterInterventionTags", value == null ? "" : value);
  }

  public String getDayIso() {
    String value = props.getProperty("planning.lastDay");
    if (value == null || value.isBlank()) {
      value = props.getProperty("dayIso");
    }
    return value;
  }

  public void setDayIso(String value) {
    if (value == null || value.isBlank()) {
      props.remove("planning.lastDay");
    } else {
      props.setProperty("planning.lastDay", value);
    }
    props.remove("dayIso");
  }

  public java.util.List<String> getBookmarkDays() {
    String raw = props.getProperty("bookmarks", "");
    if (raw.isBlank()) {
      return java.util.List.of();
    }
    java.util.List<String> result = new java.util.ArrayList<>();
    for (String token : raw.split(",")) {
      String trimmed = token.trim();
      if (!trimmed.isEmpty() && !result.contains(trimmed)) {
        result.add(trimmed);
      }
    }
    return result;
  }

  public void setBookmarkDays(java.util.List<String> days) {
    if (days == null || days.isEmpty()) {
      props.remove("bookmarks");
    } else {
      props.setProperty("bookmarks", String.join(",", days));
    }
  }

  public void addBookmarkDay(String dayIso) {
    if (dayIso == null || dayIso.isBlank()) {
      return;
    }
    java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>(getBookmarkDays());
    values.add(dayIso);
    setBookmarkDays(new java.util.ArrayList<>(values));
  }

  public void removeBookmarkDay(String dayIso) {
    if (dayIso == null || dayIso.isBlank()) {
      return;
    }
    java.util.List<String> current = new java.util.ArrayList<>(getBookmarkDays());
    if (current.remove(dayIso)) {
      setBookmarkDays(current);
    }
  }

  public String getResourceColor(String resourceId) {
    if (resourceId == null || resourceId.isBlank()) {
      return null;
    }
    return props.getProperty("resourceColor." + resourceId);
  }

  public void setResourceColor(String resourceId, String hex) {
    if (resourceId == null || resourceId.isBlank()) {
      return;
    }
    if (hex == null || hex.isBlank()) {
      props.remove("resourceColor." + resourceId);
    } else {
      props.setProperty("resourceColor." + resourceId, hex);
    }
  }

  public java.util.Map<String, String> getResourceColors() {
    java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
    for (String key : props.stringPropertyNames()) {
      if (key.startsWith("resourceColor.")) {
        String id = key.substring("resourceColor.".length());
        String value = props.getProperty(key);
        if (!id.isBlank() && value != null && !value.isBlank()) {
          map.put(id, value);
        }
      }
    }
    return map;
  }

  public boolean isTourShown() {
    return Boolean.parseBoolean(props.getProperty("tourShown", "false"));
  }

  public void setTourShown(boolean value) {
    props.setProperty("tourShown", Boolean.toString(value));
  }

  public Integer getWindowX() {
    return parseInt(props.getProperty("window.x"));
  }

  public Integer getWindowY() {
    return parseInt(props.getProperty("window.y"));
  }

  public Integer getWindowWidth() {
    return parseInt(props.getProperty("window.w"));
  }

  public Integer getWindowHeight() {
    return parseInt(props.getProperty("window.h"));
  }

  public void setWindowX(Integer value) {
    setInt("window.x", value);
  }

  public void setWindowY(Integer value) {
    setInt("window.y", value);
  }

  public void setWindowWidth(Integer value) {
    setInt("window.w", value);
  }

  public void setWindowHeight(Integer value) {
    setInt("window.h", value);
  }

  private Integer parseInt(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private void setInt(String key, Integer value) {
    if (value == null) {
      props.remove(key);
    } else {
      props.setProperty(key, Integer.toString(value));
    }
  }
}
