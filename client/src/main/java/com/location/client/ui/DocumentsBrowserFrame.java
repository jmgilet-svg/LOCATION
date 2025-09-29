package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;

/**
 * Fenêtre de raccourcis permettant d'ouvrir l'explorateur de documents filtré par type.
 */
public class DocumentsBrowserFrame extends JFrame {
  private final DataSourceProvider dataSourceProvider;

  public DocumentsBrowserFrame(DataSourceProvider dataSourceProvider) {
    super("Documents – accès rapide");
    this.dataSourceProvider = dataSourceProvider;
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(12, 12));

    Map<String, Long> counts = Collections.emptyMap();
    long total = 0L;
    try {
      List<Models.Doc> docs = dataSourceProvider.listDocs(null, null);
      if (docs != null) {
        total = docs.size();
        counts =
            docs.stream()
                .collect(Collectors.groupingBy(Models.Doc::type, Collectors.counting()));
      }
    } catch (Exception ignored) {
    }

    JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    panel.add(new JLabel("Choisissez une catégorie :"));
    panel.add(createButton(withCount("Tous les documents", total), null));
    panel.add(createButton(withCount("Devis", counts.getOrDefault("QUOTE", 0L)), "QUOTE"));
    panel.add(createButton(withCount("Bons de commande", counts.getOrDefault("ORDER", 0L)), "ORDER"));
    panel.add(
        createButton(
            withCount("Bons de livraison", counts.getOrDefault("DELIVERY", 0L)), "DELIVERY"));
    panel.add(createButton(withCount("Factures", counts.getOrDefault("INVOICE", 0L)), "INVOICE"));

    add(panel, BorderLayout.CENTER);
    pack();
    setMinimumSize(new Dimension(320, getPreferredSize().height));
    setLocationRelativeTo(null);
  }

  private JButton createButton(String label, String type) {
    return new JButton(
        new AbstractAction(label) {
          @Override
          public void actionPerformed(ActionEvent e) {
            openDocuments(type);
          }
        });
  }

  private void openDocuments(String type) {
    DocumentsFrame frame = new DocumentsFrame(dataSourceProvider, type);
    frame.setVisible(true);
    dispose();
  }

  private static String withCount(String label, long count) {
    return label + " (" + count + ")";
  }
}
