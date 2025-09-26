package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import com.location.client.core.Preferences;
import com.location.client.core.RestDataSource;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Enumeration;
import java.util.List;
import javax.swing.*;

public class MainFrame extends JFrame {
  private final DataSourceProvider dsp;
  private final Preferences prefs;
  private final PlanningPanel planning;
  private final TopBar topBar;
  private final JLabel status = new JLabel();
  private final JLabel modeBadge = new JLabel();
  private final JLabel agencyBadge = new JLabel();
  private final JMenu agencyMenu = new JMenu("Agence");
  private ButtonGroup agencyGroup = new ButtonGroup();
  private boolean updatingAgencyMenu;

  public MainFrame(DataSourceProvider dsp, Preferences prefs) {
    super("LOCATION — Planning");
    this.dsp = dsp;
    this.prefs = prefs;
    this.planning = new PlanningPanel(dsp);
    this.topBar = new TopBar(planning, prefs);

    initializeCurrentAgency();

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
    JPanel south = new JPanel(new BorderLayout());
    JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    badges.add(modeBadge);
    badges.add(new JLabel("|"));
    badges.add(agencyBadge);
    south.add(badges, BorderLayout.WEST);
    status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    south.add(status, BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);

    refreshData();

    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control N"), "new");
    getRootPane().getActionMap().put("new", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        createInterventionDialog();
      }
    });
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control E"), "editNotes");
    getRootPane().getActionMap().put("editNotes", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editNotes();
      }
    });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control shift E"), "exportCsv");
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
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control 1"), "viewDay");
    getRootPane()
        .getActionMap()
        .put(
            "viewDay",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                planning.setWeekMode(false);
                topBar.refreshCombos();
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control 2"), "viewWeek");
    getRootPane()
        .getActionMap()
        .put(
            "viewWeek",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                planning.setWeekMode(true);
                topBar.refreshCombos();
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("DELETE"), "deleteIntervention");
    getRootPane()
        .getActionMap()
        .put(
            "deleteIntervention",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                deleteSelected();
              }
            });
  }

  private JMenuBar buildMenuBar() {
    JMenuBar bar = new JMenuBar();

    JMenu file = new JMenu("Fichier");
    JMenuItem exportCsv = new JMenuItem("Exporter planning en CSV");
    exportCsv.setAccelerator(KeyStroke.getKeyStroke("control shift E"));
    exportCsv.addActionListener(e -> exportCsv());
    JMenuItem exportCsvRest = new JMenuItem("Exporter CSV (serveur REST)");
    exportCsvRest.addActionListener(e -> exportCsvRest());
    JMenuItem exportGeneral =
        new JMenuItem(
            new AbstractAction("Exporter…") {
              @Override
              public void actionPerformed(ActionEvent e) {
                new ExportDialog(MainFrame.this, dsp).setVisible(true);
              }
            });
    JMenuItem exportResources = new JMenuItem("Exporter ressources CSV");
    exportResources.addActionListener(e -> exportResourcesCsv());
    JMenuItem exportInterventionPdf = new JMenuItem("Exporter intervention (PDF)");
    exportInterventionPdf.addActionListener(e -> exportInterventionPdf());
    JMenuItem exportClients = new JMenuItem("Exporter clients CSV (REST)");
    exportClients.addActionListener(e -> exportClientsCsv());
    JMenuItem exportUnav = new JMenuItem("Exporter indisponibilités CSV (REST)");
    exportUnav.addActionListener(e -> exportUnavailabilitiesCsv());
    file.add(exportCsv);
    file.add(exportCsvRest);
    file.add(exportGeneral);
    file.add(exportResources);
    file.add(exportInterventionPdf);
    file.add(exportClients);
    file.add(exportUnav);

    JMenu data = new JMenu("Données");
    JMenuItem reset = new JMenuItem("Réinitialiser la démo (Mock)");
    reset.addActionListener(e -> {
      dsp.resetDemoData();
      refreshData();
      toast("Données de démonstration réinitialisées");
    });
    JMenuItem create = new JMenuItem("Nouvelle intervention");
    create.addActionListener(e -> createInterventionDialog());
    JMenuItem editNotes = new JMenuItem("Éditer les notes (Ctrl+E)");
    editNotes.setAccelerator(KeyStroke.getKeyStroke("control E"));
    editNotes.addActionListener(e -> editNotes());
    JMenuItem bulkEmail = new JMenuItem("Envoyer PDFs du jour (lot)");
    bulkEmail.addActionListener(e -> sendBulkForDay());
    JMenuItem newUnav = new JMenuItem("Nouvelle indisponibilité");
    newUnav.addActionListener(e -> createUnavailabilityDialog());
    JMenuItem newRecurring = new JMenuItem("Nouvelle indisponibilité récurrente");
    newRecurring.addActionListener(e -> createRecurringUnavailabilityDialog());
    JMenuItem deleteIntervention =
        new JMenuItem(
            new AbstractAction("Supprimer l'intervention sélectionnée (Suppr)") {
              @Override
              public void actionPerformed(ActionEvent e) {
                deleteSelected();
              }
            });
    JMenuItem emailPdf =
        new JMenuItem(
            new AbstractAction("Envoyer l'intervention par email… (Ctrl+M)") {
              @Override
              public void actionPerformed(ActionEvent e) {
                emailSelected();
              }
            });
    emailPdf.setAccelerator(KeyStroke.getKeyStroke("control M"));
    data.add(create);
    data.add(editNotes);
    data.add(reset);
    data.add(newUnav);
    data.add(newRecurring);
    data.add(deleteIntervention);
    data.add(emailPdf);
    data.add(bulkEmail);

    JMenu documents = new JMenu("Documents");
    JMenuItem openDocs = new JMenuItem("Ouvrir les documents commerciaux");
    openDocs.addActionListener(e -> new DocumentsFrame(dsp).setVisible(true));
    documents.add(openDocs);

    JMenu view = new JMenu("Affichage");
    JMenuItem dayView = new JMenuItem("Vue Jour (Ctrl+1)");
    dayView.setAccelerator(KeyStroke.getKeyStroke("control 1"));
    dayView.addActionListener(
        e -> {
          planning.setWeekMode(false);
          topBar.refreshCombos();
        });
    JMenuItem weekView = new JMenuItem("Vue Semaine (Ctrl+2)");
    weekView.setAccelerator(KeyStroke.getKeyStroke("control 2"));
    weekView.addActionListener(
        e -> {
          planning.setWeekMode(true);
          topBar.refreshCombos();
        });
    view.add(dayView);
    view.add(weekView);

    JMenu settings = new JMenu("Paramètres");
    JMenuItem switchSrc = new JMenuItem("Changer de source (Mock/REST)");
    switchSrc.addActionListener(e -> switchSource());
    JMenuItem cfg = new JMenuItem("Configurer le backend (URL/Login)");
    cfg.addActionListener(e -> showBackendConfig());
    JMenuItem tmpl = new JMenuItem("Modèle email (Agence)");
    tmpl.addActionListener(e -> editAgencyTemplate());
    JMenuItem loginItem =
        new JMenuItem(
            new AbstractAction("Connexion…") {
              @Override
              public void actionPerformed(ActionEvent e) {
                LoginDialog.open(MainFrame.this, dsp);
              }
            });
    JMenuItem docTmpl =
        new JMenuItem(
            new AbstractAction("Modèles email documents") {
              @Override
              public void actionPerformed(ActionEvent e) {
                new EmailTemplatesFrame(dsp).setVisible(true);
              }
            });
    JMenuItem docHtmlTmpl =
        new JMenuItem(
            new AbstractAction("Modèles document (HTML)") {
              @Override
              public void actionPerformed(ActionEvent e) {
                new DocTemplatesFrame(dsp).setVisible(true);
              }
            });
    settings.add(switchSrc);
    settings.add(cfg);
    settings.add(loginItem);
    settings.add(tmpl);
    settings.add(docTmpl);
    settings.add(docHtmlTmpl);

    JMenu help = new JMenu("Aide");
    JMenuItem about = new JMenuItem("À propos & fonctionnalités serveur");
    about.addActionListener(e -> showAbout());
    help.add(about);

    JMenu context = new JMenu("Contexte");
    context.add(agencyMenu);

    bar.add(file);
    bar.add(data);
    bar.add(documents);
    bar.add(view);
    bar.add(context);
    bar.add(settings);
    bar.add(help);
    return bar;
  }

  private void initializeCurrentAgency() {
    String saved = prefs.getCurrentAgencyId();
    if (saved != null && !saved.isBlank()) {
      dsp.setCurrentAgencyId(saved);
    } else {
      String defaultId = System.getenv().getOrDefault("LOCATION_DEFAULT_AGENCY_ID", "A1");
      if (defaultId != null && !defaultId.isBlank()) {
        dsp.setCurrentAgencyId(defaultId);
      }
    }
  }

  private void refreshData() {
    planning.reload();
    populateAgencyMenu(planning.getAgencies());
    planning.repaint();
    topBar.refreshCombos();
    updateBadges();
    prefs.setCurrentAgencyId(dsp.getCurrentAgencyId());
    prefs.save();
  }

  private void populateAgencyMenu(List<Models.Agency> agencies) {
    updatingAgencyMenu = true;
    try {
      agencyMenu.removeAll();
      agencyGroup = new ButtonGroup();
      String current = dsp.getCurrentAgencyId();
      boolean found = false;
      for (Models.Agency agency : agencies) {
        String agencyId = agency.id();
        JRadioButtonMenuItem item =
            new JRadioButtonMenuItem(agency.name() + " (" + agencyId + ")");
        item.setActionCommand(agencyId);
        item.addActionListener(e -> onAgencyMenuSelected(agencyId));
        agencyGroup.add(item);
        agencyMenu.add(item);
        if (agencyId != null && agencyId.equals(current)) {
          item.setSelected(true);
          found = true;
        }
      }
      if (!found && !agencies.isEmpty()) {
        Models.Agency first = agencies.get(0);
        dsp.setCurrentAgencyId(first.id());
        current = first.id();
        prefs.setCurrentAgencyId(current);
        prefs.save();
        selectAgencyButton(current);
      }
    } finally {
      updatingAgencyMenu = false;
    }
  }

  private void selectAgencyButton(String agencyId) {
    if (agencyId == null) {
      return;
    }
    Enumeration<AbstractButton> buttons = agencyGroup.getElements();
    while (buttons.hasMoreElements()) {
      AbstractButton button = buttons.nextElement();
      if (agencyId.equals(button.getActionCommand())) {
        button.setSelected(true);
        break;
      }
    }
  }

  private void onAgencyMenuSelected(String agencyId) {
    if (updatingAgencyMenu || agencyId == null || agencyId.isBlank()) {
      return;
    }
    dsp.setCurrentAgencyId(agencyId);
    prefs.setCurrentAgencyId(agencyId);
    prefs.save();
    refreshData();
  }

  private void updateBadges() {
    modeBadge.setText(dsp instanceof RestDataSource ? "Backend REST" : "Mode Démo (Mock)");
    String agencyId = dsp.getCurrentAgencyId();
    agencyBadge.setText("Agence: " + (agencyId == null || agencyId.isBlank() ? "—" : agencyId));
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

  private void exportResourcesCsv() {
    try {
      Path tmp = Files.createTempFile("resources-", ".csv");
      dsp.downloadResourcesCsv(topBar.getTags(), tmp);
      toast("Ressources exportées: " + tmp);
      Desktop.getDesktop().open(tmp.toFile());
    } catch (Exception ex) {
      error("Export ressources → " + ex.getMessage());
    }
  }

  private void exportClientsCsv() {
    if (!(dsp instanceof RestDataSource)) {
      error("Export clients CSV disponible uniquement en mode REST.");
      return;
    }
    try {
      Path tmp = Files.createTempFile("clients-", ".csv");
      dsp.downloadClientsCsv(tmp);
      Desktop.getDesktop().open(tmp.toFile());
    } catch (Exception ex) {
      error("Export clients → " + ex.getMessage());
    }
  }

  private void exportUnavailabilitiesCsv() {
    if (!(dsp instanceof RestDataSource)) {
      error("Export indisponibilités CSV disponible uniquement en mode REST.");
      return;
    }
    try {
      Path tmp = Files.createTempFile("unavailabilities-", ".csv");
      dsp.downloadUnavailabilitiesCsv(topBar.getFrom(), topBar.getTo(), topBar.getResourceId(), tmp);
      Desktop.getDesktop().open(tmp.toFile());
    } catch (Exception ex) {
      error("Export indisponibilités → " + ex.getMessage());
    }
  }

  private void showAbout() {
    java.util.Map<String, Boolean> features;
    try {
      features = dsp.getServerFeatures();
    } catch (Exception ex) {
      features = java.util.Map.of();
    }
    StringBuilder sb = new StringBuilder();
    sb.append("LOCATION\n");
    sb.append("Source actuelle: ").append(dsp.getLabel()).append('\n');
    sb.append('\n').append("Fonctionnalités serveur:\n");
    if (features.isEmpty()) {
      sb.append("  (indisponible)\n");
    } else {
      features.entrySet().stream()
          .sorted(java.util.Map.Entry.comparingByKey())
          .forEach(entry -> sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n'));
    }
    JTextArea ta = new JTextArea(sb.toString());
    ta.setEditable(false);
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    ta.setFont(new JTextArea().getFont());
    JScrollPane scroll = new JScrollPane(ta);
    scroll.setPreferredSize(new java.awt.Dimension(360, 240));
    JOptionPane.showMessageDialog(this, scroll, "À propos", JOptionPane.INFORMATION_MESSAGE);
  }

  private String escape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace(";", ",");
  }

  private void exportInterventionPdf() {
    Models.Intervention sel = planning.getSelected();
    if (sel == null) {
      toast("Sélectionnez une intervention dans le planning.");
      return;
    }
    if (!(dsp instanceof RestDataSource)) {
      toast("Export PDF disponible uniquement en mode REST.");
      return;
    }
    try {
      Path out = Files.createTempFile("intervention-" + sel.id() + "-", ".pdf");
      dsp.downloadInterventionPdf(sel.id(), out);
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
    JTextArea taBody = new JTextArea("Bonjour,\nVeuillez trouver l'intervention en pièce jointe.\nCordialement.");
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
    int result = JOptionPane.showConfirmDialog(this, panel, "Envoyer l'intervention par email", JOptionPane.OK_CANCEL_OPTION);
    if (result == JOptionPane.OK_OPTION) {
      String to = tfTo.getText().trim();
      String subject = tfSubject.getText();
      String body = taBody.getText();
      prefs.setLastEmailTo(to);
      prefs.save();
      try {
        dsp.emailInterventionPdf(sel.id(), to, subject, body);
        if (dsp instanceof RestDataSource) {
          toast("Email envoyé (202 Accepted)");
        } else {
          toast("Email simulé (mode Mock)");
        }
      } catch (Exception ex) {
        error(ex.getMessage());
      }
    }
  }

  private void deleteSelected() {
    Models.Intervention sel = planning.getSelected();
    if (sel == null) {
      JOptionPane.showMessageDialog(this, "Aucune intervention sélectionnée.");
      return;
    }
    int choice =
        JOptionPane.showConfirmDialog(
            this,
            "Supprimer l'intervention \"" + sel.title() + "\" ?",
            "Confirmation",
            JOptionPane.OK_CANCEL_OPTION);
    if (choice == JOptionPane.OK_OPTION) {
      boolean done = planning.deleteSelected();
      if (done) {
        toast("Intervention supprimée");
      }
    }
  }

  private void editNotes() {
    Models.Intervention sel = planning.getSelected();
    if (sel == null) {
      JOptionPane.showMessageDialog(this, "Sélectionnez une intervention.");
      return;
    }
    JTextArea ta = new JTextArea(sel.notes() == null ? "" : sel.notes());
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    ta.setRows(12);
    JScrollPane scroll = new JScrollPane(ta);
    int result =
        JOptionPane.showConfirmDialog(
            this,
            scroll,
            "Notes pour: " + sel.title(),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
    if (result == JOptionPane.OK_OPTION) {
      Models.Intervention updated =
          new Models.Intervention(
              sel.id(),
              sel.agencyId(),
              sel.resourceId(),
              sel.clientId(),
              sel.driverId(),
              sel.title(),
              sel.start(),
              sel.end(),
              ta.getText());
      try {
        dsp.updateIntervention(updated);
        toast("Notes enregistrées");
        refreshData();
      } catch (Exception ex) {
        error("Impossible d'enregistrer: " + ex.getMessage());
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
                null,
                tfTitle.getText(),
                Instant.parse(tfStart.getText()),
                Instant.parse(tfEnd.getText()),
                null));
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
                Instant.parse(tfEnd.getText()),
                false));
        toast("Indisponibilité créée");
        refreshData();
      } catch (Exception ex) {
        error(ex.getMessage());
      }
    }
  }

  private void createRecurringUnavailabilityDialog() {
    List<Models.Resource> resources = planning.getResources();
    if (resources.isEmpty()) {
      toast("Aucune ressource");
      return;
    }
    JComboBox<Models.Resource> cbR = new JComboBox<>(resources.toArray(new Models.Resource[0]));
    JComboBox<DayOfWeek> cbDay = new JComboBox<>(DayOfWeek.values());
    JTextField tfReason = new JTextField("Maintenance hebdo");
    JTextField tfStart = new JTextField("08:00");
    JTextField tfEnd = new JTextField("10:00");
    JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
    panel.add(new JLabel("Ressource:"));
    panel.add(cbR);
    panel.add(new JLabel("Jour:"));
    panel.add(cbDay);
    panel.add(new JLabel("Raison:"));
    panel.add(tfReason);
    panel.add(new JLabel("Début (HH:mm):"));
    panel.add(tfStart);
    panel.add(new JLabel("Fin (HH:mm):"));
    panel.add(tfEnd);
    int ok =
        JOptionPane.showConfirmDialog(
            this, panel, "Créer une indisponibilité récurrente", JOptionPane.OK_CANCEL_OPTION);
    if (ok == JOptionPane.OK_OPTION) {
      Models.Resource resource = (Models.Resource) cbR.getSelectedItem();
      DayOfWeek day = (DayOfWeek) cbDay.getSelectedItem();
      if (resource == null || day == null) {
        error("Sélection invalide");
        return;
      }
      try {
        dsp.createRecurringUnavailability(
            new Models.RecurringUnavailability(
                null,
                resource.id(),
                day,
                LocalTime.parse(tfStart.getText()),
                LocalTime.parse(tfEnd.getText()),
                tfReason.getText()));
        toast("Indisponibilité récurrente créée");
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

  private void sendBulkForDay() {
    List<Models.Intervention> items = planning.getInterventions();
    if (items == null || items.isEmpty()) {
      toast("Aucune intervention pour ce jour");
      return;
    }
    String input = JOptionPane.showInputDialog(this, "Destinataire (optionnel) :", "Envoi groupé", JOptionPane.QUESTION_MESSAGE);
    if (input == null) {
      return;
    }
    String to = input.trim();
    if (to.isEmpty()) {
      to = null;
    }
    try {
      dsp.emailBulk(items.stream().map(Models.Intervention::id).toList(), to);
      toast("Envoi groupé demandé");
    } catch (Exception ex) {
      error("Échec envoi groupé : " + ex.getMessage());
    }
  }

  private void editAgencyTemplate() {
    List<Models.Agency> agencies = dsp.listAgencies();
    if (agencies.isEmpty()) {
      toast("Aucune agence");
      return;
    }
    Models.Agency selected = (Models.Agency)
        JOptionPane.showInputDialog(this, "Choisir l'agence", "Modèle email", JOptionPane.PLAIN_MESSAGE, null, agencies.toArray(), agencies.get(0));
    if (selected == null) {
      return;
    }
    Models.EmailTemplate template = dsp.getAgencyEmailTemplate(selected.id());
    JTextField subjectField = new JTextField(template != null && template.subject() != null ? template.subject() : "Intervention {{interventionTitle}}");
    JTextArea bodyField = new JTextArea(template != null && template.body() != null ? template.body() : "Bonjour {{clientName}},\nVeuillez trouver la fiche d'intervention.\nAgence : {{agencyName}}\nDu {{start}} au {{end}}");
    bodyField.setRows(10);
    bodyField.setWrapStyleWord(true);
    bodyField.setLineWrap(true);
    JPanel panel = new JPanel(new BorderLayout(6, 6));
    JPanel top = new JPanel(new BorderLayout(6, 6));
    top.add(new JLabel("Sujet :"), BorderLayout.WEST);
    top.add(subjectField, BorderLayout.CENTER);
    panel.add(top, BorderLayout.NORTH);
    panel.add(new JScrollPane(bodyField), BorderLayout.CENTER);
    int ok = JOptionPane.showConfirmDialog(this, panel, "Modèle email — " + selected.name(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (ok == JOptionPane.OK_OPTION) {
      try {
        dsp.updateAgencyEmailTemplate(selected.id(), new Models.EmailTemplate(subjectField.getText(), bodyField.getText()));
        toast("Modèle enregistré");
      } catch (Exception ex) {
        error("Échec sauvegarde : " + ex.getMessage());
      }
    }
  }

  private void toast(String message) {
    status.setText(message);
  }

  private void error(String message) {
    status.setText("⚠️ " + message);
  }
}
