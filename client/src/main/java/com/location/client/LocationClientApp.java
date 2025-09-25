package com.location.client;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.MockDataSource;
import com.location.client.core.RestDataSource;
import com.location.client.core.SelectionDialog;
import com.location.client.ui.MainFrame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class LocationClientApp {

  public static void main(String[] args) {
    Locale.setDefault(Locale.FRANCE);
    new LocationClientApp().start(args);
  }

  private void start(String[] args) {
    String forced = parseForcedDataSource(args);
    String stored = readStoredDataSource();
    String chosen = forced != null ? forced : (stored != null ? stored : null);
    boolean remember = stored != null;

    if (chosen == null) {
      var sel = SelectionDialog.showAndGet();
      if (sel == null) System.exit(0);
      chosen = sel.mode();
      remember = sel.remember();
    }
    if (remember && forced == null) {
      storeDataSource(chosen);
    }

    DataSourceProvider provider = createProvider(chosen);
    MainFrame.open(provider);
  }

  private static String parseForcedDataSource(String[] args) {
    for (String a : args) {
      if (a.startsWith("--datasource=")) {
        String v = a.substring("--datasource=".length()).trim().toLowerCase();
        if (List.of("mock", "rest").contains(v)) return v;
      }
    }
    return null;
  }

  private static String readStoredDataSource() {
    try {
      Path dir = Path.of(System.getProperty("user.home"), ".location");
      Path props = dir.resolve("app.properties");
      if (!Files.exists(props)) return null;
      Properties p = new Properties();
      try (var in = new FileInputStream(props.toFile())) {
        p.load(in);
        String v = p.getProperty("datasource");
        if (v == null) return null;
        v = v.trim().toLowerCase();
        if (Arrays.asList("mock", "rest").contains(v)) return v;
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  public static void storeDataSource(String mode) {
    try {
      Path dir = Path.of(System.getProperty("user.home"), ".location");
      Files.createDirectories(dir);
      File props = dir.resolve("app.properties").toFile();
      Properties p = new Properties();
      if (props.exists()) {
        try (var in = new FileInputStream(props)) {
          p.load(in);
        }
      }
      p.setProperty("datasource", mode);
      try (var out = new FileOutputStream(props)) {
        p.store(out, "LOCATION preferences");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private DataSourceProvider createProvider(String mode) {
    if ("rest".equalsIgnoreCase(mode)) {
      var env = System.getenv();
      String base = env.getOrDefault("LOCATION_API_BASE", env.getOrDefault("LOCATION_BACKEND_URL", "http://localhost:8080"));
      return new RestDataSource(base);
    }
    return new MockDataSource();
  }
}
