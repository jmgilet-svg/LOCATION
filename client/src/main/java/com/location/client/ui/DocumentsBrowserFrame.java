package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.*;

/**
 * Fenêtre de raccourcis permettant d'ouvrir l'explorateur de documents filtré par type.
 */
public class DocumentsBrowserFrame extends JFrame {
  private static final Logger LOGGER = Logger.getLogger(DocumentsBrowserFrame.class.getName());
  private final DataSourceProvider dataSourceProvider;

  public DocumentsBrowserFrame(DataSourceProvider dataSourceProvider) {
    super("Documents – accès rapide");
    this.dataSourceProvider = dataSourceProvider;
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(12, 12));

    Map<String, Long> counts = Collections.emptyMap();
    long total = 0L;
    long deliveriesPending = 0L;
    long invoicesUnpaid = 0L;
    try {
      List<Models.Doc> docs = dataSourceProvider.listDocs(null, null);
      if (docs != null) {
        total = docs.size();
        counts =
            docs.stream()
                .collect(Collectors.groupingBy(Models.Doc::type, Collectors.counting()));
        deliveriesPending =
            docs.stream().filter(d -> "DELIVERY".equals(d.type()) && !d.delivered()).count();
        invoicesUnpaid =
            docs.stream().filter(d -> "INVOICE".equals(d.type()) && !d.paid()).count();
      }
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Impossible de charger les documents", ex);
    }

    JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    panel.add(new JLabel("Choisissez une catégorie :"));
    panel.add(createButton(withCount("Tous les documents", total), null));
    panel.add(createButton(withCount("Devis", counts.getOrDefault("QUOTE", 0L)), "QUOTE"));
    panel.add(createButton(withCount("Bons de commande", counts.getOrDefault("ORDER", 0L)), "ORDER"));
    String deliveriesLabel = withCount("Bons de livraison", counts.getOrDefault("DELIVERY", 0L));
    if (deliveriesPending > 0) {
      deliveriesLabel += " — en attente: " + deliveriesPending;
    }
    panel.add(createButton(deliveriesLabel, "DELIVERY"));
    String invoicesLabel = withCount("Factures", counts.getOrDefault("INVOICE", 0L));
    if (invoicesUnpaid > 0) {
      invoicesLabel += " — impayées: " + invoicesUnpaid;
    }
    panel.add(createButton(invoicesLabel, "INVOICE"));

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
