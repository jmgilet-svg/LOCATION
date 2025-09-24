package com.location.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.location.client.core.*;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.swing.*;

public class LocationClientApp {

  public static void main(String[] args) {
    Locale.setDefault(Locale.FRANCE);
    FlatLightLaf.setup();
    EventQueue.invokeLater(() -> new LocationClientApp().start(args));
  }

  private JFrame frame;
  private JLabel statusBadge;
  private DataSourceProvider dataSource;

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

    this.dataSource = createProvider(chosen);
    buildUI();
  }

  private void buildUI() {
    frame = new JFrame("LOCATION — Demo Shell");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setSize(1000, 700);

    var menuBar = new JMenuBar();

    var menuData = new JMenu("Données");
    var resetDemo = new JMenuItem("Réinitialiser la démo");
    resetDemo.addActionListener(
        e -> {
          dataSource.resetDemoData();
          JOptionPane.showMessageDialog(frame, "Données de démonstration réinitialisées.");
        });
    menuData.add(resetDemo);

    var menuSettings = new JMenu("Paramètres");
    var sourceItem = new JMenuItem("Source de données…");
    sourceItem.addActionListener(
        e -> {
          var sel = SelectionDialog.showAndGet();
          if (sel != null) {
            if (sel.remember()) storeDataSource(sel.mode());
            JOptionPane.showMessageDialog(
                frame, "Source changée en " + sel.mode() + ". Redémarrage conseillé.");
          }
        });
    menuSettings.add(sourceItem);

    menuBar.add(new JMenu("Fichier"));
    menuBar.add(menuData);
    menuBar.add(menuSettings);
    menuBar.add(new JMenu("Aide"));

    frame.setJMenuBar(menuBar);

    var content = new JPanel(new BorderLayout());
    statusBadge = new JLabel("  " + dataSource.getLabel() + "  ");
    statusBadge.setOpaque(true);
    statusBadge.setHorizontalAlignment(SwingConstants.RIGHT);
    statusBadge.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    statusBadge.setBackground("MOCK".equals(dataSource.getLabel()) ? new java.awt.Color(0xE8F5E9) : new java.awt.Color(0xE3F2FD));
    content.add(statusBadge, BorderLayout.NORTH);

    // Placeholder centre (listes simples pour démonstration Diff 1)
    JTextArea area = new JTextArea();
    area.setEditable(false);
    area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    area.setText(renderDemoLists());
    content.add(new JScrollPane(area), BorderLayout.CENTER);

    frame.setContentPane(content);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private String renderDemoLists() {
    StringBuilder sb = new StringBuilder();
    sb.append("Agences:\n");
    dataSource.listAgencies().forEach(a -> sb.append(" - ").append(a.name()).append("\n"));
    sb.append("\nClients:\n");
    dataSource.listClients().forEach(c -> sb.append(" - ").append(c.name()).append(" <").append(c.billingEmail()).append(">\n"));
    sb.append("\n(Écrans complets arrivent en Diff 3)\n");
    return sb.toString();
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

  private static void storeDataSource(String mode) {
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
      return new RestDataSource(System.getenv().getOrDefault("LOCATION_BACKEND_URL", "http://localhost:8080"));
    }
    return new MockDataSource();
  }
}
