package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import com.location.client.core.Preferences;
import com.location.client.core.RestDataSource;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.swing.*;

public class MainFrame extends JFrame {
  private final DataSourceProvider dsp;
  private final Preferences prefs;
  private final PlanningPanel planning;
  private final TopBar topBar;
  private final JLabel status = new JLabel();

  public MainFrame(DataSourceProvider dsp, Preferences prefs) {
    super("LOCATION — Planning");
    this.dsp = dsp;
    this.prefs = prefs;
    this.planning = new PlanningPanel(dsp);
    this.topBar = new TopBar(planning, prefs);

    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        try {
          dsp.close();
        } catch (Exception ignored) {
        }
      }
    });
    setSize(1100, 720);
    setLocationRelativeTo(null);

    setJMenuBar(buildMenuBar());
    add(topBar, BorderLayout.NORTH);
    add(planning, BorderLayout.CENTER);
    status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    add(status, BorderLayout.SOUTH);

    refreshData();
    status.setText("Source: " + dsp.getLabel());

    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control N"), "new");
    getRootPane().getActionMap().put("new", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        createInterventionDialog();
      }
    });
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control E"), "exportCsv");
    getRootPane().getActionMap().put("exportCsv", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        exportCsv();
      }
    });
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control L"), "login");
    getRootPane().getActionMap().put("login", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showBackendConfig();
      }
    });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control LEFT"), "prevDay");
    getRootPane()
        .getActionMap()
        .put(
            "prevDay",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                topBar.prevDay();
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control RIGHT"), "nextDay");
    getRootPane()
        .getActionMap()
        .put(
            "nextDay",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                topBar.nextDay();
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control I"), "newUnav");
    getRootPane()
        .getActionMap()
        .put(
            "newUnav",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                createUnavailabilityDialog();
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control M"), "emailDoc");
    getRootPane()
        .getActionMap()
        .put(
            "emailDoc",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                emailSelected();
              }
            });
  }

  private JMenuBar buildMenuBar() {
    JMenuBar bar = new JMenuBar();

    JMenu file = new JMenu("Fichier");
    JMenuItem exportCsv = new JMenuItem("Exporter planning en CSV");
    exportCsv.addActionListener(e -> exportCsv());
    JMenuItem exportCsvRest = new JMenuItem("Exporter CSV (serveur REST)");
    exportCsvRest.addActionListener(e -> exportCsvRest());
    JMenuItem exportPdf = new JMenuItem("Exporter PDF (stub)");
    exportPdf.addActionListener(e -> exportPdfStub());
    JMenuItem emailPdf =
        new JMenuItem(
            new AbstractAction("Envoyer PDF par email… (Ctrl+M)") {
              @Override
              public void actionPerformed(ActionEvent e) {
                emailSelected();
              }
            });
    emailPdf.setAccelerator(KeyStroke.getKeyStroke("control M"));
    file.add(exportCsv);
    file.add(exportCsvRest);
    file.add(exportPdf);
    file.add(emailPdf);

    JMenu data = new JMenu("Données");
    JMenuItem reset = new JMenuItem("Réinitialiser la démo (Mock)");
    reset.addActionListener(e -> {
      dsp.resetDemoData();
      refreshData();
      toast("Données de démonstration réinitialisées");
    });
    JMenuItem create = new JMenuItem("Nouvelle intervention");
    create.addActionListener(e -> createInterventionDialog());
    JMenuItem newUnav = new JMenuItem("Nouvelle indisponibilité");
    newUnav.addActionListener(e -> createUnavailabilityDialog());
    data.add(create);
    data.add(reset);
    data.add(newUnav);

    JMenu settings = new JMenu("Paramètres");
    JMenuItem switchSrc = new JMenuItem("Changer de source (Mock/REST)");
    switchSrc.addActionListener(e -> switchSource());
    JMenuItem cfg = new JMenuItem("Configurer le backend (URL/Login)");
    cfg.addActionListener(e -> showBackendConfig());
    settings.add(switchSrc);
    settings.add(cfg);

    bar.add(file);
    bar.add(data);
    bar.add(settings);
    return bar;
  }

  private void refreshData() {
    planning.reload();
    planning.repaint();
    topBar.refreshCombos();
  }

  private void exportCsv() {
    try {
      Path tmp = Files.createTempFile("planning-", ".csv");
      List<Models.Intervention> items = planning.getInterventions();
      StringBuilder sb =
          new StringBuilder("id;title;agencyId;resourceId;clientId;start;end\n");
      for (Models.Intervention i : items) {
        sb.append(i.id())
            .append(';')
            .append(escape(i.title()))
            .append(';')
            .append(escape(i.agencyId()))
            .append(';')
            .append(escape(i.resourceId()))
            .append(';')
            .append(escape(i.clientId()))
            .append(';')
            .append(i.start())
            .append(';')
            .append(i.end())
            .append('\n');
      }
      Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
      toast("Export CSV: " + tmp);
      Desktop.getDesktop().open(tmp.toFile());
    } catch (IOException ex) {
      error("Export CSV → " + ex.getMessage());
    }
  }

  private void exportCsvRest() {
    if (!(dsp instanceof RestDataSource rd)) {
      toast("Export CSV REST nécessite le mode REST");
      return;
    }
    try {
      Path tmp = Files.createTempFile("planning-rest-", ".csv");
      rd.downloadCsvInterventions(
          topBar.getFrom(), topBar.getTo(), topBar.getResourceId(), topBar.getClientId(), topBar.getQuery(), tmp);
      Desktop.getDesktop().open(tmp.toFile());
    } catch (Exception ex) {
      error("Export CSV REST → " + ex.getMessage());
    }
  }

  private String escape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace(";", ",");
  }

  private void exportPdfStub() {
    if (!(dsp instanceof RestDataSource rd)) {
      toast("PDF (stub) nécessite le mode REST");
      return;
    }
    try {
      Path out = Files.createTempFile("doc-", ".pdf");
      rd.downloadPdfStub("demo", out);
      toast("PDF exporté: " + out);
      Desktop.getDesktop().open(out.toFile());
    } catch (Exception ex) {
      error("Export PDF → " + ex.getMessage());
    }
  }

  private void emailSelected() {
    Models.Intervention sel = planning.getSelected();
    if (sel == null) {
      JOptionPane.showMessageDialog(
          this,
          "Sélectionnez d'abord une intervention (clic sur une tuile).",
          "Information",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    JTextField tfTo = new JTextField(prefs.getLastEmailTo());
    JTextField tfSubject = new JTextField("Intervention " + sel.title());
    JTextArea taBody = new JTextArea("Bonjour,\nVeuillez trouver le document en pièce jointe.\nCordialement.");
    taBody.setLineWrap(true);
    taBody.setWrapStyleWord(true);
    taBody.setRows(6);
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    JPanel fields = new JPanel(new GridLayout(0, 1, 4, 4));
    fields.add(new JLabel("Destinataire:"));
    fields.add(tfTo);
    fields.add(new JLabel("Objet:"));
    fields.add(tfSubject);
    panel.add(fields, BorderLayout.NORTH);
    panel.add(new JScrollPane(taBody), BorderLayout.CENTER);
    int result = JOptionPane.showConfirmDialog(this, panel, "Envoyer PDF par email", JOptionPane.OK_CANCEL_OPTION);
    if (result == JOptionPane.OK_OPTION) {
      String to = tfTo.getText().trim();
      String subject = tfSubject.getText();
      String body = taBody.getText();
      prefs.setLastEmailTo(to);
      prefs.save();
      if (dsp instanceof RestDataSource rd) {
        try {
          rd.emailDocument(sel.id(), to, subject, body);
          toast("Email envoyé (202 Accepted)");
        } catch (Exception ex) {
          error(ex.getMessage());
        }
      } else {
        JOptionPane.showMessageDialog(this, "(MOCK) Email simulé vers " + to);
      }
    }
  }

  private void createInterventionDialog() {
    List<Models.Resource> resources = planning.getResources();
    List<Models.Client> clients = planning.getClients();
    if (resources.isEmpty() || clients.isEmpty()) {
      toast("Ressources/Clients vides");
      return;
    }
    JComboBox<Models.Resource> cbR = new JComboBox<>(resources.toArray(new Models.Resource[0]));
    JComboBox<Models.Client> cbC = new JComboBox<>(clients.toArray(new Models.Client[0]));
    JTextField tfTitle = new JTextField("Nouvelle intervention");
    Instant start = Instant.now().plus(Duration.ofHours(1));
    JTextField tfStart = new JTextField(start.toString());
    JTextField tfEnd = new JTextField(start.plus(Duration.ofHours(2)).toString());
    JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
    panel.add(new JLabel("Ressource:"));
    panel.add(cbR);
    panel.add(new JLabel("Client:"));
    panel.add(cbC);
    panel.add(new JLabel("Titre:"));
    panel.add(tfTitle);
    panel.add(new JLabel("Début (ISO):"));
    panel.add(tfStart);
    panel.add(new JLabel("Fin (ISO):"));
    panel.add(tfEnd);
    int result = JOptionPane.showConfirmDialog(this, panel, "Créer une intervention", JOptionPane.OK_CANCEL_OPTION);
    if (result == JOptionPane.OK_OPTION) {
      Models.Resource resource = (Models.Resource) cbR.getSelectedItem();
      Models.Client client = (Models.Client) cbC.getSelectedItem();
      if (resource == null || client == null) {
        error("Sélection invalide");
        return;
      }
      try {
        dsp.createIntervention(
            new Models.Intervention(
                null,
                resource.agencyId(),
                resource.id(),
                client.id(),
                tfTitle.getText(),
                Instant.parse(tfStart.getText()),
                Instant.parse(tfEnd.getText())));
        toast("Intervention créée");
        refreshData();
      } catch (Exception ex) {
        error(ex.getMessage());
      }
    }
  }

  private void createUnavailabilityDialog() {
    List<Models.Resource> resources = planning.getResources();
    if (resources.isEmpty()) {
      toast("Aucune ressource");
      return;
    }
    JComboBox<Models.Resource> cbR = new JComboBox<>(resources.toArray(new Models.Resource[0]));
    JTextField tfReason = new JTextField("Maintenance");
    Instant start = Instant.now().plus(Duration.ofHours(1));
    JTextField tfStart = new JTextField(start.toString());
    JTextField tfEnd = new JTextField(start.plus(Duration.ofHours(2)).toString());
    JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
    panel.add(new JLabel("Ressource:"));
    panel.add(cbR);
    panel.add(new JLabel("Raison:"));
    panel.add(tfReason);
    panel.add(new JLabel("Début (ISO):"));
    panel.add(tfStart);
    panel.add(new JLabel("Fin (ISO):"));
    panel.add(tfEnd);
    int result =
        JOptionPane.showConfirmDialog(this, panel, "Créer une indisponibilité", JOptionPane.OK_CANCEL_OPTION);
    if (result == JOptionPane.OK_OPTION) {
      Models.Resource resource = (Models.Resource) cbR.getSelectedItem();
      if (resource == null) {
        error("Sélection invalide");
        return;
      }
      try {
        dsp.createUnavailability(
            new Models.Unavailability(
                null,
                resource.id(),
                tfReason.getText(),
                Instant.parse(tfStart.getText()),
                Instant.parse(tfEnd.getText())));
        toast("Indisponibilité créée");
        refreshData();
      } catch (Exception ex) {
        error(ex.getMessage());
      }
    }
  }

  private void switchSource() {
    String[] options = {"Mock", "REST", "Annuler"};
    int choice = JOptionPane.showOptionDialog(
        this,
        "Choisir la source de données.\n(Nécessite un redémarrage de l'app)",
        "Paramètres",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        options,
        options[0]);
    if (choice == 0) {
      prefs.setLastSource("mock");
      prefs.save();
    } else if (choice == 1) {
      prefs.setLastSource("rest");
      prefs.save();
    }
    if (choice == 0 || choice == 1) {
      JOptionPane.showMessageDialog(this, "Relancez l'application pour appliquer la modification.");
    }
  }

  private void showBackendConfig() {
    JTextField tfUrl = new JTextField(prefs.getBaseUrl());
    JTextField tfUser = new JTextField(prefs.getRestUser());
    JPasswordField tfPass = new JPasswordField(prefs.getRestPass());
    JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
    panel.add(new JLabel("Base URL:"));
    panel.add(tfUrl);
    panel.add(new JLabel("Utilisateur:"));
    panel.add(tfUser);
    panel.add(new JLabel("Mot de passe:"));
    panel.add(tfPass);
    int ok = JOptionPane.showConfirmDialog(this, panel, "Backend REST", JOptionPane.OK_CANCEL_OPTION);
    if (ok == JOptionPane.OK_OPTION) {
      prefs.setBaseUrl(tfUrl.getText());
      prefs.setRestUser(tfUser.getText());
      prefs.setRestPass(new String(tfPass.getPassword()));
      prefs.save();
      if (dsp instanceof RestDataSource rd) {
        rd.configure(prefs.getBaseUrl(), prefs.getRestUser(), prefs.getRestPass());
      }
      toast("Configuration enregistrée");
    }
  }

  private void toast(String message) {
    status.setText(message);
  }

  private void error(String message) {
    status.setText("⚠️ " + message);
  }
}
