package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class DocTemplatesFrame extends JFrame {
  private final DataSourceProvider dataSourceProvider;
  private final JComboBox<String> docTypeCombo =
      new JComboBox<>(new String[] {"QUOTE", "ORDER", "DELIVERY", "INVOICE"});
  private final JTextArea htmlArea = new JTextArea(20, 80);

  public DocTemplatesFrame(DataSourceProvider dataSourceProvider) {
    super("Modèles document (HTML) — Agence: " + dataSourceProvider.getCurrentAgencyId());
    this.dataSourceProvider = dataSourceProvider;

    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setPreferredSize(new Dimension(900, 520));
    setLayout(new BorderLayout(8, 8));

    JPanel north = new JPanel();
    north.add(new JLabel("Type:"));
    north.add(docTypeCombo);
    JButton loadButton = new JButton(new AbstractAction("Charger") {
      @Override
      public void actionPerformed(ActionEvent e) {
        loadTemplate();
      }
    });
    JButton saveButton = new JButton(new AbstractAction("Enregistrer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        saveTemplate();
      }
    });
    north.add(loadButton);
    north.add(saveButton);
    add(north, BorderLayout.NORTH);

    htmlArea.setLineWrap(true);
    htmlArea.setWrapStyleWord(true);
    add(new JScrollPane(htmlArea), BorderLayout.CENTER);

    JPanel footer = new JPanel(new BorderLayout());
    footer.add(
        new JLabel(
            "Placeholders disponibles: {{agencyName}}, {{clientName}}, {{docRef}}, {{docTitle}}, {{docType}}, {{docDate}}, {{totalTtc}}"),
        BorderLayout.CENTER);
    add(footer, BorderLayout.SOUTH);

    pack();
    setLocationRelativeTo(null);
    loadTemplate();

    docTypeCombo.addActionListener(e -> loadTemplate());
  }

  private void loadTemplate() {
    Models.DocTemplate template =
        dataSourceProvider.getDocTemplate((String) docTypeCombo.getSelectedItem());
    htmlArea.setText(template.html());
    htmlArea.setCaretPosition(0);
  }

  private void saveTemplate() {
    String html = htmlArea.getText();
    try {
      dataSourceProvider.saveDocTemplate((String) docTypeCombo.getSelectedItem(), html);
      JOptionPane.showMessageDialog(this, "Modèle enregistré.");
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(
          this, "Échec de l'enregistrement : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }
}
