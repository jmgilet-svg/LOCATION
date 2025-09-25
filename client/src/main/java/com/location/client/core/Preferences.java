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
}
