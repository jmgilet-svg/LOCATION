package com.location.client.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.location.client.LocationClientApp;
import com.location.client.core.DataSourceProvider;
import com.location.client.core.SelectionDialog;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.Objects;
import javax.swing.*;

public final class MainFrame {
  private final DataSourceProvider dataSource;
  private JFrame frame;

  private MainFrame(DataSourceProvider dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  public static void open(DataSourceProvider dataSource) {
    FlatLightLaf.setup();
    EventQueue.invokeLater(() -> new MainFrame(dataSource).show());
  }

  private void show() {
    frame = new JFrame("LOCATION — Demo Shell");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosed(java.awt.event.WindowEvent e) {
        try {
          dataSource.close();
        } catch (Exception ignored) {
        }
      }
    });
    frame.setSize(1000, 700);

    frame.setJMenuBar(buildMenuBar());

    var content = new JPanel(new BorderLayout());
    JLabel statusBadge = new JLabel("  " + dataSource.getLabel() + "  ");
    statusBadge.setOpaque(true);
    statusBadge.setHorizontalAlignment(SwingConstants.RIGHT);
    statusBadge.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    statusBadge.setBackground(
        "MOCK".equals(dataSource.getLabel())
            ? new java.awt.Color(0xE8F5E9)
            : new java.awt.Color(0xE3F2FD));
    content.add(statusBadge, BorderLayout.NORTH);

    JTextArea area = new JTextArea();
    area.setEditable(false);
    area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    area.setText(renderDemoLists());
    content.add(new JScrollPane(area), BorderLayout.CENTER);

    frame.setContentPane(content);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JMenuBar buildMenuBar() {
    var menuBar = new JMenuBar();

    menuBar.add(new JMenu("Fichier"));

    var menuData = new JMenu("Données");
    var resetDemo = new JMenuItem("Réinitialiser la démo");
    resetDemo.addActionListener(
        e -> {
          dataSource.resetDemoData();
          JOptionPane.showMessageDialog(frame, "Données de démonstration réinitialisées.");
        });
    menuData.add(resetDemo);
    menuBar.add(menuData);

    var menuSettings = new JMenu("Paramètres");
    var sourceItem = new JMenuItem("Source de données…");
    sourceItem.addActionListener(
        e -> {
          var sel = SelectionDialog.showAndGet();
          if (sel != null) {
            if (sel.remember()) LocationClientApp.storeDataSource(sel.mode());
            JOptionPane.showMessageDialog(
                frame, "Source changée en " + sel.mode() + ". Redémarrage conseillé.");
          }
        });
    menuSettings.add(sourceItem);
    menuBar.add(menuSettings);

    menuBar.add(new JMenu("Aide"));

    return menuBar;
  }

  private String renderDemoLists() {
    StringBuilder sb = new StringBuilder();
    sb.append("Agences:\n");
    dataSource.listAgencies().forEach(a -> sb.append(" - ").append(a.name()).append("\n"));
    sb.append("\nClients:\n");
    dataSource
        .listClients()
        .forEach(c -> sb.append(" - ").append(c.name()).append(" <").append(c.billingEmail()).append(">\n"));
    sb.append("\n(Écrans complets arrivent en Diff 3)\n");
    return sb.toString();
  }
}
