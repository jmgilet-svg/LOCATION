package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.MockDataSource;
import com.location.client.core.RestDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/** Point d'entrée autonome pour Diff 3 (respecte --datasource et préférences). */
public final class StandalonePlanner {
  private StandalonePlanner() {}

  public static void main(String[] args) {
    Locale.setDefault(Locale.FRANCE);
    String forced = parseArg(args, "--datasource=");
    String mode = forced != null ? forced : readPreference("datasource", "mock");
    DataSourceProvider provider = createProvider(mode);
    MainFrame.open(provider);
  }

  private static DataSourceProvider createProvider(String mode) {
    if ("rest".equalsIgnoreCase(mode)) {
      var env = System.getenv();
      String base = env.getOrDefault("LOCATION_API_BASE", env.getOrDefault("LOCATION_BACKEND_URL", "http://localhost:8080"));
      return new RestDataSource(base);
    }
    return new MockDataSource();
  }

  private static String parseArg(String[] args, String key) {
    for (String a : args) {
      if (a != null && a.startsWith(key)) {
        return a.substring(key.length()).trim();
      }
    }
    return null;
  }

  private static String readPreference(String key, String def) {
    try {
      Path dir = Path.of(System.getProperty("user.home"), ".location");
      Path props = dir.resolve("app.properties");
      if (!Files.exists(props)) {
        return def;
      }
      Properties p = new Properties();
      try (var in = Files.newInputStream(props)) {
        p.load(in);
      }
      String value = p.getProperty(key);
      if (value == null) {
        return def;
      }
      value = value.trim();
      return value.isEmpty() ? def : value;
    } catch (IOException e) {
      return def;
    }
  }
}
