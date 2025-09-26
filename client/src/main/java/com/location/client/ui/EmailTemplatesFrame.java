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
import javax.swing.JTextField;

public class EmailTemplatesFrame extends JFrame {

  private final DataSourceProvider dataSource;
  private final JComboBox<String> typeCombo =
      new JComboBox<>(new String[] {"QUOTE", "ORDER", "DELIVERY", "INVOICE"});
  private final JTextField subjectField = new JTextField();
  private final JTextArea bodyArea = new JTextArea(12, 60);

  public EmailTemplatesFrame(DataSourceProvider dataSource) {
    super("Modèles email documents — Agence " + dataSource.getCurrentAgencyId());
    this.dataSource = dataSource;
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setPreferredSize(new Dimension(720, 420));
    setLayout(new BorderLayout(8, 8));

    JPanel north = new JPanel();
    north.add(new JLabel("Type:"));
    north.add(typeCombo);
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

    JPanel center = new JPanel(new BorderLayout(6, 6));
    center.add(new JLabel("Sujet:"), BorderLayout.NORTH);
    center.add(subjectField, BorderLayout.CENTER);

    bodyArea.setLineWrap(true);
    bodyArea.setWrapStyleWord(true);
    JPanel bodyPanel = new JPanel(new BorderLayout(6, 6));
    bodyPanel.add(
        new JLabel(
            "Corps (variables: {{agencyName}}, {{clientName}}, {{docRef}}, {{docTitle}}, {{docType}}, {{docDate}}, {{totalTtc}})"),
        BorderLayout.NORTH);
    bodyPanel.add(new JScrollPane(bodyArea), BorderLayout.CENTER);
    center.add(bodyPanel, BorderLayout.SOUTH);
    add(center, BorderLayout.CENTER);

    pack();
    setLocationRelativeTo(null);
    loadTemplate();
  }

  private void loadTemplate() {
    String type = (String) typeCombo.getSelectedItem();
    Models.EmailTemplate template = dataSource.getEmailTemplate(type);
    subjectField.setText(template.subject());
    bodyArea.setText(template.body());
  }

  private void saveTemplate() {
    String type = (String) typeCombo.getSelectedItem();
    try {
      Models.EmailTemplate saved =
          dataSource.saveEmailTemplate(type, subjectField.getText(), bodyArea.getText());
      subjectField.setText(saved.subject());
      bodyArea.setText(saved.body());
      JOptionPane.showMessageDialog(this, "Modèle sauvegardé.");
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Échec sauvegarde: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }
}
