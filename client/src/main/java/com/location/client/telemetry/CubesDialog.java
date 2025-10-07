package com.location.client.telemetry;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/**
 * D2 — Visualisation des « cubes » de métriques (Agence / Type / Client).
 */
public final class CubesDialog extends JDialog {
  private final Metrics metrics = Metrics.get();
  private final JTable table;
  private final CubeModel model;
  private final JComboBox<Dim> dimensionSelect;
  private final JLabel subtitle;

  public CubesDialog(Component parent) {
    super(SwingUtilities.getWindowAncestor(parent), "Cubes de métriques", ModalityType.MODELESS);
    setUndecorated(true);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    getRootPane()
        .setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 80), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
    setLayout(new BorderLayout(8, 8));

    JPanel header = new JPanel(new BorderLayout(8, 8));
    dimensionSelect = new JComboBox<>(Dim.values());
    subtitle = new JLabel("Regroupement par Agence");
    header.add(dimensionSelect, BorderLayout.WEST);
    header.add(subtitle, BorderLayout.CENTER);
    add(header, BorderLayout.NORTH);

    model = new CubeModel(new ArrayList<>());
    table = new JTable(model);
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    add(new JScrollPane(table), BorderLayout.CENTER);

    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    JButton refreshButton = new JButton("Rafraîchir");
    JButton exportButton = new JButton("Exporter CSV");
    JButton closeButton = new JButton("Fermer");
    refreshButton.addActionListener(e -> refreshData());
    exportButton.addActionListener(e -> exportCsv());
    closeButton.addActionListener(e -> setVisible(false));
    footer.add(refreshButton);
    footer.add(exportButton);
    footer.add(closeButton);
    add(footer, BorderLayout.SOUTH);

    setSize(720, 420);
    centerOn(parent);

    dimensionSelect.addActionListener(
        e -> {
          Dim dim = (Dim) dimensionSelect.getSelectedItem();
          subtitle.setText(
              "Regroupement par " + (dim == null ? Dim.AGENCY.label : dim.label));
          refreshData();
        });

    refreshData();
  }

  private void centerOn(Component parent) {
    if (parent == null) {
      setLocationRelativeTo(null);
      return;
    }
    java.awt.Point p = parent.getLocationOnScreen();
    int x = p.x + Math.max(0, (parent.getWidth() - getWidth()) / 2);
    int y = p.y + Math.max(0, (parent.getHeight() - getHeight()) / 2);
    setLocation(x, y);
  }

  private void refreshData() {
    Dim dim = (Dim) dimensionSelect.getSelectedItem();
    if (dim == null) {
      dim = Dim.AGENCY;
    }
    Map<String, List<Long>> grouped = new HashMap<>();
    List<Metrics.Event> events = metrics.recentEvents();
    for (Metrics.Event event : events) {
      if (event == null || event.getType() == null) {
        continue;
      }
      if (!"conflict.resolved".equals(event.getType())) {
        continue;
      }
      Map<String, String> attributes = event.getAttributes();
      if (attributes == null) {
        attributes = Map.of();
      }
      String agency = attributes.getOrDefault("agency", "");
      String resourceType = attributes.getOrDefault("rtype", "");
      String clients = attributes.getOrDefault("clients", "");
      long ttr = 0L;
      try {
        ttr = Long.parseLong(attributes.getOrDefault("ttr.ms", "0"));
      } catch (NumberFormatException ignored) {
        ttr = 0L;
      }
      switch (dim) {
        case AGENCY ->
            grouped.computeIfAbsent(emptyToNA(agency), key -> new ArrayList<>()).add(ttr);
        case RTYPE ->
            grouped.computeIfAbsent(emptyToNA(resourceType), key -> new ArrayList<>()).add(ttr);
        case CLIENT -> {
          if (clients == null || clients.isBlank()) {
            grouped.computeIfAbsent("N/A", key -> new ArrayList<>()).add(ttr);
          } else {
            for (String client : clients.split("\\|")) {
              if (client == null || client.isBlank()) {
                continue;
              }
              grouped.computeIfAbsent(client, key -> new ArrayList<>()).add(ttr);
            }
          }
        }
      }
    }

    List<Row> rows = new ArrayList<>();
    for (Map.Entry<String, List<Long>> entry : grouped.entrySet()) {
      List<Long> values = entry.getValue();
      if (values == null || values.isEmpty()) {
        continue;
      }
      Collections.sort(values);
      double avg = values.stream().mapToLong(v -> v).average().orElse(0d);
      long p50 = percentile(values, 0.50d);
      long p90 = percentile(values, 0.90d);
      rows.add(new Row(entry.getKey(), values.size(), avg, p50, p90));
    }
    rows.sort((a, b) -> Long.compare(b.p90Ms, a.p90Ms));
    model.setData(rows);
  }

  private void exportCsv() {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Exporter les cubes (CSV)");
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    java.io.File file = chooser.getSelectedFile();
    if (file == null) {
      return;
    }
    try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
      writer.write("dimension,rows,count,avg_ms,p50_ms,p90_ms,generated_at\n");
      long now = Instant.now().toEpochMilli();
      List<Row> rows = model.getData();
      Dim dim = (Dim) dimensionSelect.getSelectedItem();
      String dimensionLabel = dim == null ? Dim.AGENCY.label : dim.label;
      for (Row row : rows) {
        String escapedKey = row.key.replace("\"", "\"\"");
        writer.write(
            String.format(
                "\"%s\",\"%s\",%d,%.1f,%d,%d,%d\n",
                dimensionLabel,
                escapedKey,
                row.count,
                row.avgMs,
                row.p50Ms,
                row.p90Ms,
                now));
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(
          this,
          "Erreur lors de l'export: " + ex.getMessage(),
          "Export CSV",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private static String emptyToNA(String value) {
    if (value == null || value.isBlank()) {
      return "N/A";
    }
    return value;
  }

  private static long percentile(List<Long> sortedAsc, double percentile) {
    if (sortedAsc == null || sortedAsc.isEmpty()) {
      return 0L;
    }
    int index = (int) Math.round((sortedAsc.size() - 1) * percentile);
    index = Math.max(0, Math.min(sortedAsc.size() - 1, index));
    return sortedAsc.get(index);
  }

  private enum Dim {
    AGENCY("Agence"),
    RTYPE("Type ressource"),
    CLIENT("Client");

    final String label;

    Dim(String label) {
      this.label = label;
    }
  }

  private static final class Row {
    final String key;
    final int count;
    final double avgMs;
    final long p50Ms;
    final long p90Ms;

    Row(String key, int count, double avgMs, long p50Ms, long p90Ms) {
      this.key = key;
      this.count = count;
      this.avgMs = avgMs;
      this.p50Ms = p50Ms;
      this.p90Ms = p90Ms;
    }
  }

  private static final class CubeModel extends AbstractTableModel {
    private List<Row> rows;

    CubeModel(List<Row> rows) {
      this.rows = rows;
    }

    void setData(List<Row> rows) {
      this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
      fireTableDataChanged();
    }

    List<Row> getData() {
      return rows == null ? List.of() : rows;
    }

    @Override
    public int getRowCount() {
      return rows == null ? 0 : rows.size();
    }

    @Override
    public int getColumnCount() {
      return 5;
    }

    @Override
    public String getColumnName(int column) {
      return switch (column) {
        case 0 -> "Clé";
        case 1 -> "# Résolutions";
        case 2 -> "TTR moyen (ms)";
        case 3 -> "P50 (ms)";
        default -> "P90 (ms)";
      };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      Row row = rows.get(rowIndex);
      return switch (columnIndex) {
        case 0 -> row.key;
        case 1 -> row.count;
        case 2 -> String.format("%.1f", row.avgMs);
        case 3 -> row.p50Ms;
        default -> row.p90Ms;
      };
    }
  }
}
