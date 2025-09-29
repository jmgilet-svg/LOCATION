package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class AgencySettingsFrame extends JFrame {
  private final DataSourceProvider dsp;
  private Models.Agency agency;
  private final JTextField tfName = new JTextField(24);
  private final JTextArea taLegal = new JTextArea(4, 24);
  private final JTextField tfIban = new JTextField(24);
  private final JLabel lbLogo = new JLabel("Aucun logo");
  private final JTextField tfTemplateSubject = new JTextField(28);
  private final JTextArea taTemplateBody = new JTextArea(6, 28);

  public AgencySettingsFrame(DataSourceProvider dsp, Models.Agency agency) {
    super("Paramètres de l'agence");
    this.dsp = dsp;
    this.agency = agency;
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(8, 8));

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(4, 6, 4, 6);
    c.anchor = GridBagConstraints.WEST;
    int y = 0;

    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Nom"), c);
    c.gridx = 1;
    form.add(tfName, c);
    y++;

    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Mentions légales"), c);
    c.gridx = 1;
    taLegal.setLineWrap(true);
    taLegal.setWrapStyleWord(true);
    form.add(new JScrollPane(taLegal), c);
    y++;

    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("IBAN"), c);
    c.gridx = 1;
    form.add(tfIban, c);
    y++;

    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Logo (PNG)"), c);
    c.gridx = 1;
    form.add(lbLogo, c);
    y++;

    JButton btLogo = new JButton(new AbstractAction("Choisir…") {
      @Override
      public void actionPerformed(ActionEvent e) {
        pickLogo();
      }
    });
    JButton btClearLogo = new JButton(new AbstractAction("Supprimer le logo") {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearLogo();
      }
    });
    JPanel logoButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    logoButtons.add(btLogo);
    logoButtons.add(btClearLogo);
    c.gridx = 1;
    c.gridy = y;
    form.add(logoButtons, c);

    JPanel templatePanel = new JPanel(new GridBagLayout());
    templatePanel.setBorder(BorderFactory.createTitledBorder("Modèle e-mail DOC_MAIL"));
    GridBagConstraints t = new GridBagConstraints();
    t.insets = new Insets(4, 6, 4, 6);
    t.anchor = GridBagConstraints.WEST;
    int ty = 0;

    t.gridx = 0;
    t.gridy = ty;
    templatePanel.add(new JLabel("Sujet"), t);
    t.gridx = 1;
    templatePanel.add(tfTemplateSubject, t);
    ty++;

    t.gridx = 0;
    t.gridy = ty;
    templatePanel.add(new JLabel("Corps (HTML)"), t);
    t.gridx = 1;
    taTemplateBody.setLineWrap(true);
    taTemplateBody.setWrapStyleWord(true);
    templatePanel.add(new JScrollPane(taTemplateBody), t);
    ty++;

    JButton btSaveTemplate = new JButton(new AbstractAction("Enregistrer le modèle") {
      @Override
      public void actionPerformed(ActionEvent e) {
        saveTemplate();
      }
    });
    t.gridx = 1;
    t.gridy = ty;
    templatePanel.add(btSaveTemplate, t);

    JButton btSaveAgency = new JButton(new AbstractAction("Enregistrer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        saveAgency();
      }
    });

    add(form, BorderLayout.NORTH);
    add(templatePanel, BorderLayout.CENTER);
    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    south.add(btSaveAgency);
    add(south, BorderLayout.SOUTH);

    loadData();

    setSize(760, 560);
    setLocationRelativeTo(null);
  }

  private void loadData() {
    try {
      if (agency != null && agency.id() != null) {
        Models.Agency latest = dsp.getAgency(agency.id());
        if (latest != null) {
          agency = latest;
        }
      }
    } catch (RuntimeException ex) {
      // Ignore network errors during initial load, keep existing data.
    }
    tfName.setText(agency == null ? "" : valueOrEmpty(agency.name()));
    taLegal.setText(agency == null ? "" : valueOrEmpty(agency.legalFooter()));
    tfIban.setText(agency == null ? "" : valueOrEmpty(agency.iban()));
    updateLogoLabel();

    Models.EmailTemplate template =
        agency == null || agency.id() == null
            ? null
            : dsp.getAgencyEmailTemplate(agency.id(), "DOC_MAIL");
    if (template != null) {
      tfTemplateSubject.setText(valueOrEmpty(template.subject()));
      taTemplateBody.setText(valueOrEmpty(template.html()));
    } else {
      tfTemplateSubject.setText("Votre document {{number}}");
      taTemplateBody.setText(
          "<p>Bonjour,<br/>Veuillez trouver ci-joint le document {{number}} ({{type}}) pour {{client}}.</p>");
    }
  }

  private void updateLogoLabel() {
    if (agency != null && agency.logoDataUri() != null && !agency.logoDataUri().isBlank()) {
      lbLogo.setText("Logo chargé");
    } else {
      lbLogo.setText("Aucun logo");
    }
  }

  private void pickLogo() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter("Images PNG", "png"));
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      try {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        agency =
            new Models.Agency(
                agency.id(), agency.name(), agency.legalFooter(), agency.iban(), dataUri);
        updateLogoLabel();
      } catch (Exception ex) {
        Toast.error(this, "Impossible de charger le logo: " + ex.getMessage());
      }
    }
  }

  private void clearLogo() {
    if (agency == null) {
      return;
    }
    agency = new Models.Agency(agency.id(), agency.name(), agency.legalFooter(), agency.iban(), null);
    updateLogoLabel();
  }

  private void saveAgency() {
    try {
      if (agency == null) {
        agency = new Models.Agency(null, null, null, null, null);
      }
      Models.Agency updated =
          new Models.Agency(
              agency.id(),
              tfName.getText().trim(),
              emptyToNull(taLegal.getText()),
              emptyToNull(tfIban.getText()),
              agency.logoDataUri());
      agency = dsp.saveAgency(updated);
      Toast.success(this, "Agence enregistrée");
    } catch (RuntimeException ex) {
      Toast.error(this, ex.getMessage());
    }
  }

  private void saveTemplate() {
    if (agency == null || agency.id() == null) {
      Toast.error(this, "Enregistrez l'agence avant le modèle e-mail.");
      return;
    }
    try {
      dsp.updateAgencyEmailTemplate(
          agency.id(),
          "DOC_MAIL",
          tfTemplateSubject.getText(),
          taTemplateBody.getText());
      Toast.success(this, "Modèle enregistré");
    } catch (RuntimeException ex) {
      Toast.error(this, ex.getMessage());
    }
  }

  private static String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String emptyToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
