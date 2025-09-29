package com.location.client.ui;

import com.location.client.core.DataSourceProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

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

    JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    panel.add(new JLabel("Choisissez une catégorie :"));
    panel.add(createButton("Tous les documents", null));
    panel.add(createButton("Devis", "QUOTE"));
    panel.add(createButton("Bons de commande", "ORDER"));
    panel.add(createButton("Bons de livraison", "DELIVERY"));
    panel.add(createButton("Factures", "INVOICE"));

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
}
