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

  public String getDayIso() {
    return props.getProperty("dayIso");
  }

  public void setDayIso(String value) {
    if (value == null) {
      props.remove("dayIso");
    } else {
      props.setProperty("dayIso", value);
    }
  }

  public boolean isTourShown() {
    return Boolean.parseBoolean(props.getProperty("tourShown", "false"));
  }

  public void setTourShown(boolean value) {
    props.setProperty("tourShown", Boolean.toString(value));
  }
}
