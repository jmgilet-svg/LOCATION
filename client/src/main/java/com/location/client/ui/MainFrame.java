package com.location.client.ui;

import com.location.client.core.ConflictUtil;
import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import com.location.client.core.Preferences;
import com.location.client.core.RestDataSource;
import com.location.client.ui.i18n.Language;
import com.location.client.ui.uikit.EmptyState;
import com.location.client.ui.uikit.Icons;
import com.location.client.ui.uikit.Notify;
import com.location.client.ui.uikit.Svg;
import com.location.client.ui.uikit.Toasts;
import com.location.client.ui.uikit.Ui;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

public class MainFrame extends JFrame {
  private final DataSourceProvider dsp;
  private final Preferences prefs;
  private final PlanningPanel planning;
  private final PlanningMinimap minimap;
  private final PlanningInspector inspector;
  private final JPanel centerCards = new JPanel(new java.awt.CardLayout());
  private JPanel clientsPanel;
  private JPanel unavPanel;
  private final TopBar topBar;
  private final Sidebar sidebar;
  private final JToolBar selectionBar = new JToolBar();
  private final JButton selectionInfo = new JButton();
  private final JButton activityButton = new JButton("Activité");
  private final JLabel connectionBadge = new JLabel();
  private final JLabel status = new JLabel();
  private final JLabel modeBadge = new JLabel();
  private final JLabel agencyBadge = new JLabel();
  private final JMenu agencyMenu = new JMenu("Agence");
  private final JMenu bookmarksMenu = new JMenu("Signets");
  private ButtonGroup agencyGroup = new ButtonGroup();
  private boolean updatingAgencyMenu;
  private final Timer heartbeat;
  private Timer badgeTimer;
  private JDialog activityDialog;
  private GuidedTour guidedTour;
  private boolean restoringGeometry;

  public MainFrame(DataSourceProvider dsp, Preferences prefs) {
    super("LOCATION — Planning");

    final java.beans.PropertyChangeListener networkListener =
        evt -> Ui.ensure(() -> Toasts.error(MainFrame.this, String.valueOf(evt.getNewValue())));
    Notify.on("network.error", networkListener);
    this.dsp = dsp;
    this.prefs = prefs;
    ResourceColors.initialize(prefs);
    this.planning = new PlanningPanel(dsp);
    this.minimap = new PlanningMinimap();
    this.inspector = new PlanningInspector(dsp);
    this.topBar = new TopBar(planning, prefs);
    this.sidebar = new Sidebar(this::handleNavigation);
    minimap.setWorkingHours(planning.getStartHour(), planning.getEndHour());
    planning.addReloadListener(this::updateMinimap);

    final java.beans.PropertyChangeListener conflictsResolver =
        evt -> {
          Object v = evt.getNewValue();
          if (v instanceof ConflictUtil.Conflict c) {
            Ui.ensure(() -> planning.resolveConflict(c));
          }
        };
    Notify.on("conflicts.resolve", conflictsResolver);

    initializeCurrentAgency();

    if (dsp instanceof RestDataSource rest) {
      rest.startPingThread();
      connectionBadge.setText("🟡 REST — tentative de connexion…");
      heartbeat = new Timer(1000, e -> updateConnectionBadge(rest));
      heartbeat.start();
    } else {
      connectionBadge.setText("🟣 Mock — source de données locale");
      Timer mockTimer = new Timer(2000, e -> connectionBadge.setText("🟣 Mock — source de données locale"));
      mockTimer.setRepeats(false);
      mockTimer.start();
      heartbeat = mockTimer;
    }

    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    restoreWindowBounds();
    addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentMoved(ComponentEvent e) {
            saveGeometry();
          }

          @Override
          public void componentResized(ComponentEvent e) {
            saveGeometry();
          }
        });
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowOpened(WindowEvent e) {
            if (!prefs.isTourShown()) {
              SwingUtilities.invokeLater(MainFrame.this::startGuidedTour);
            }
          }

          @Override
          public void windowClosed(WindowEvent e) {
            heartbeat.stop();
            if (badgeTimer != null) {
              badgeTimer.stop();
            }
            Notify.off("network.error", networkListener);
            Notify.off("conflicts.resolve", conflictsResolver);
            try {
              dsp.close();
            } catch (Exception ignored) {
            }
          }
        });

    setJMenuBar(buildMenuBar());
    add(topBar, BorderLayout.NORTH);
    add(sidebar, BorderLayout.WEST);
    JPanel planningContainer = new JPanel(new BorderLayout());
    inspector.setPreferredSize(new Dimension(320, 600));
    planningContainer.add(planning, BorderLayout.CENTER);
    JTabbedPane eastTabs = new JTabbedPane();
    ConflictsPanel conflictsPanel = new ConflictsPanel();
    eastTabs.addTab("Inspecteur", inspector);
    eastTabs.addTab("Conflits", conflictsPanel);
    planningContainer.add(eastTabs, BorderLayout.EAST);
    java.beans.PropertyChangeListener l_conf =
        evt -> {
          Object v = evt.getNewValue();
          if (v instanceof java.util.List<?> list
              && (list.isEmpty() || list.get(0) instanceof ConflictUtil.Conflict)) {
            conflictsPanel.setConflicts((java.util.List<ConflictUtil.Conflict>) v);
            eastTabs.setTitleAt(
                1, list.isEmpty() ? "Conflits" : "Conflits (" + list.size() + ")");
          } else if (v == null) {
            conflictsPanel.setConflicts(null);
            eastTabs.setTitleAt(1, "Conflits");
          }
        };
    Notify.on("conflicts.update", l_conf);
    this.addWindowListener(
        new java.awt.event.WindowAdapter() {
          @Override
          public void windowClosed(java.awt.event.WindowEvent e) {
            Notify.off("conflicts.update", l_conf);
          }
        });
    planningContainer.add(minimap, BorderLayout.SOUTH);

    centerCards.add(planningContainer, "planning");
    SuggestionPanel suggestionPanel = new SuggestionPanel(dsp);
    suggestionPanel.setHours(planning.getStartHour(), planning.getEndHour());
    suggestionPanel.setAfterApply(planning::reload);
    selectionBar.setFloatable(false);
    selectionInfo.setFocusable(false);
    selectionInfo.setEnabled(false);
    selectionInfo.setBorderPainted(false);
    selectionInfo.setContentAreaFilled(false);
    selectionInfo.setHorizontalAlignment(SwingConstants.LEFT);
    selectionInfo.setText("Aucune sélection");
    selectionBar.add(selectionInfo);
    selectionBar.addSeparator();
    selectionBar.add(
        new JButton(
            new AbstractAction("Dupliquer") {
              @Override
              public void actionPerformed(ActionEvent e) {
                planning.duplicateSelected();
              }
            }));
    selectionBar.add(
        new JButton(
            new AbstractAction("Dupliquer +1j") {
              @Override
              public void actionPerformed(ActionEvent e) {
                planning.duplicateSelected(1);
              }
            }));
    selectionBar.add(
        new JButton(
            new AbstractAction("Dupliquer +7j") {
              @Override
              public void actionPerformed(ActionEvent e) {
                planning.duplicateSelected(7);
              }
            }));
    selectionBar.add(
        new JButton(
            new AbstractAction("−30 min") {
              @Override
              public void actionPerformed(ActionEvent e) {
                planning.shiftSelection(Duration.ofMinutes(-30));
              }
            }));
    selectionBar.add(
        new JButton(
            new AbstractAction("+30 min") {
              @Override
              public void actionPerformed(ActionEvent e) {
                planning.shiftSelection(Duration.ofMinutes(30));
              }
            }));
    selectionBar.add(
        new JButton(
            new AbstractAction("Supprimer") {
              @Override
              public void actionPerformed(ActionEvent e) {
                deleteSelected();
              }
            }));
    selectionBar.setVisible(false);
    planning.addSelectionListener(
        (selection, dayItems) -> {
          suggestionPanel.showFor(selection, dayItems);
          boolean hasSelection = !selection.isEmpty();
          selectionBar.setVisible(hasSelection);
          inspector.showIntervention(hasSelection ? selection.get(0) : null);
          if (hasSelection) {
            int count = selection.size();
            selectionInfo.setText(count == 1 ? "1 sélectionnée" : count + " sélectionnées");
          } else {
            selectionInfo.setText("Aucune sélection");
          }
          prefs.setDayIso(planning.getDay().toString());
          prefs.save();
        });
    add(centerCards, BorderLayout.CENTER);
    add(suggestionPanel, BorderLayout.EAST);
    sidebar.setSelected("planning");
    showCard("planning");
    registerNavigationShortcuts();
    startBadgeTimer();
    JPanel south = new JPanel(new BorderLayout());
    JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    badges.add(connectionBadge);
    badges.add(new JLabel("|"));
    badges.add(modeBadge);
    badges.add(new JLabel("|"));
    badges.add(agencyBadge);
    south.add(badges, BorderLayout.WEST);
    status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    south.add(status, BorderLayout.CENTER);
    activityButton.setFocusable(false);
    activityButton.addActionListener(e -> openActivity());
    south.add(activityButton, BorderLayout.EAST);
    JPanel bottom = new JPanel(new BorderLayout());
    bottom.add(selectionBar, BorderLayout.NORTH);
    bottom.add(south, BorderLayout.SOUTH);
    add(bottom, BorderLayout.SOUTH);

    refreshData();

    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control N"), "new");
    getRootPane().getActionMap().put("new", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        createInterventionDialog();
      }
    });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control D"), "duplicate");
    getRootPane()
        .getActionMap()
        .put(
            "duplicate",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                planning.duplicateSelected();
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control E"), "exportDialog");
    getRootPane()
        .getActionMap()
        .put(
            "exportDialog",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                new ExportDialog(MainFrame.this, dsp).setVisible(true);
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control shift E"), "exportPlanningCsv");
    getRootPane().getActionMap().put("exportPlanningCsv", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        exportPlanningDayCsvDialog();
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
        .put(KeyStroke.getKeyStroke("control Z"), "undoHistory");
    getRootPane()
        .getActionMap()
        .put(
            "undoHistory",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                planning.undoLast();
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control Y"), "redoHistory");
    getRootPane()
        .getActionMap()
        .put(
            "redoHistory",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                planning.redoLast();
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
        .put(KeyStroke.getKeyStroke("alt LEFT"), "prevDayAlt");
    getRootPane()
        .getActionMap()
        .put(
            "prevDayAlt",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                topBar.prevDay();
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("alt RIGHT"), "nextDayAlt");
    getRootPane()
        .getActionMap()
        .put(
            "nextDayAlt",
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
    InputMap paletteInput = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    paletteInput.put(KeyStroke.getKeyStroke("control K"), "commandPalette");
    paletteInput.put(KeyStroke.getKeyStroke("meta K"), "commandPalette");
    getRootPane()
        .getActionMap()
        .put(
            "commandPalette",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                openCommandPalette();
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control F"), "globalSearch");
    getRootPane()
        .getActionMap()
        .put(
            "globalSearch",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                openGlobalSearch();
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("F1"), "shortcutHelp");
    getRootPane()
        .getActionMap()
        .put(
            "shortcutHelp",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                new CheatSheetDialog(MainFrame.this).setVisible(true);
              }
            });
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control alt L"), "themeLight");
    getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("control alt D"), "themeDark");
    getRootPane()
        .getActionMap()
        .put(
            "themeLight",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Theme.apply(Theme.Mode.LIGHT);
              }
            });
    getRootPane()
        .getActionMap()
        .put(
            "themeDark",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Theme.apply(Theme.Mode.DARK);
              }
            });
  }

  private JMenu buildConflictsMenu() {
    JMenu conflicts = new JMenu("Conflits");
    conflicts.addMenuListener(
        new javax.swing.event.MenuListener() {
          @Override
          public void menuSelected(javax.swing.event.MenuEvent e) {
            try {
              int n = planning.getConflicts() == null ? 0 : planning.getConflicts().size();
              conflicts.setText("Conflits (" + n + ")");
            } catch (Exception ignored) {
            }
          }

          @Override
          public void menuDeselected(javax.swing.event.MenuEvent e) {}

          @Override
          public void menuCanceled(javax.swing.event.MenuEvent e) {}
        });
    JMenuItem next = new JMenuItem("Aller au prochain conflit");
    next.setAccelerator(
        KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_RIGHT,
            java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.ALT_DOWN_MASK));
    next.addActionListener(e -> planning.nextConflict());
    JMenuItem prev = new JMenuItem("Aller au conflit précédent");
    prev.setAccelerator(
        KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_LEFT,
            java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.ALT_DOWN_MASK));
    prev.addActionListener(e -> planning.prevConflict());
    JCheckBoxMenuItem only = new JCheckBoxMenuItem("Afficher uniquement les conflits");
    only.setSelected(planning.isFilterOnlyConflicts());
    only.addActionListener(e -> planning.setFilterOnlyConflicts(only.isSelected()));
    conflicts.add(next);
    conflicts.add(prev);
    conflicts.addSeparator();
    conflicts.add(only);
    planning.addReloadListener(() -> only.setSelected(planning.isFilterOnlyConflicts()));
    return conflicts;
  }

  private JMenuBar buildMenuBar() {
    JMenuBar bar = new JMenuBar();
    bar.add(buildConflictsMenu());

    JMenu file = new JMenu(Language.tr("menu.file"));
    file.setMnemonic('F');
    JMenuItem exportPlanningDay = new JMenuItem(Language.tr("export.csv.planning"));
    exportPlanningDay.setAccelerator(KeyStroke.getKeyStroke("control shift E"));
    exportPlanningDay.addActionListener(e -> exportPlanningDayCsvDialog());
    JMenuItem exportClientsCsv = new JMenuItem(Language.tr("export.csv.clients"));
    exportClientsCsv.addActionListener(e -> exportClientsCsvDialog());
    JMenuItem exportIcs = new JMenuItem("Export ICS (Planning jour)");
    exportIcs.addActionListener(e -> exportPlanningDayIcsDialog());
    JMenuItem exportPng = new JMenuItem("Export PNG (Planning)");
    exportPng.addActionListener(e -> exportPlanningPngDialog());
    JMenuItem exportPlanningPdf = new JMenuItem("Export PDF (Planning complet)");
    exportPlanningPdf.addActionListener(e -> exportPlanningPdfFullDialog());
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
    exportGeneral.setAccelerator(KeyStroke.getKeyStroke("control E"));
    JMenuItem exportResources = new JMenuItem("Exporter ressources CSV");
    exportResources.addActionListener(e -> exportResourcesCsv());
    JMenuItem exportInterventionPdf = new JMenuItem("Exporter intervention (PDF)");
    exportInterventionPdf.addActionListener(e -> exportInterventionPdf());
    JMenuItem exportClientsRest = new JMenuItem("Exporter clients CSV (REST)");
    exportClientsRest.addActionListener(e -> exportClientsCsvRest());
    JMenuItem exportUnav = new JMenuItem("Exporter indisponibilités CSV (REST)");
    exportUnav.addActionListener(e -> exportUnavailabilitiesCsv());
    file.add(exportPlanningDay);
    file.add(exportClientsCsv);
    file.add(exportIcs);
    file.add(exportPng);
    file.add(exportPlanningPdf);
    file.addSeparator();
    file.add(exportCsvRest);
    file.add(exportGeneral);
    file.add(exportResources);
    file.add(exportInterventionPdf);
    file.add(exportClientsRest);
    file.add(exportUnav);

    JMenu data = new JMenu(Language.tr("menu.data"));
    data.setMnemonic('D');
    JMenuItem create = new JMenuItem("Nouvelle intervention");
    create.setAccelerator(KeyStroke.getKeyStroke("control N"));
    create.addActionListener(e -> createInterventionDialog());
    JMenuItem editNotes = new JMenuItem("Éditer les notes");
    editNotes.addActionListener(e -> editNotes());
    JMenuItem bulkEmail = new JMenuItem("Envoyer PDFs du jour (lot)");
    bulkEmail.addActionListener(e -> sendBulkForDay());
    JMenuItem newUnav = new JMenuItem("Nouvelle indisponibilité");
    newUnav.addActionListener(e -> createUnavailabilityDialog());
    JMenuItem newRecurring = new JMenuItem("Nouvelle indisponibilité récurrente");
    newRecurring.addActionListener(e -> createRecurringUnavailabilityDialog());
    JMenuItem manageResources =
        new JMenuItem(
            new AbstractAction("Gérer les ressources…") {
              @Override
              public void actionPerformed(ActionEvent e) {
                new ResourceEditorFrame(dsp).setVisible(true);
              }
            });
    JMenuItem exploreResources =
        new JMenuItem(
            new AbstractAction("Explorateur de ressources…") {
              @Override
              public void actionPerformed(ActionEvent e) {
                try {
                  new ResourceExplorerFrame(dsp).setVisible(true);
                } catch (RuntimeException ex) {
                  Toasts.error(MainFrame.this, "Explorateur indisponible: " + ex.getMessage());
                }
              }
            });
    JMenuItem manageUnav =
        new JMenuItem(
            new AbstractAction("Gérer les indisponibilités…") {
              @Override
              public void actionPerformed(ActionEvent e) {
                new UnavailabilityFrame(dsp).setVisible(true);
              }
            });
    JMenuItem deleteIntervention =
        new JMenuItem(
            new AbstractAction("Supprimer l'intervention sélectionnée (Suppr)") {
              @Override
              public void actionPerformed(ActionEvent e) {
                deleteSelected();
              }
            });
    deleteIntervention.setAccelerator(KeyStroke.getKeyStroke("DELETE"));
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
    data.add(newUnav);
    data.add(newRecurring);
    data.add(manageResources);
    data.add(exploreResources);
    data.add(manageUnav);
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
    JMenuItem viewActivity = new JMenuItem("Activité récente");
    viewActivity.addActionListener(e -> openActivity());
    view.add(viewActivity);
    view.addSeparator();
    view.add(bookmarksMenu);

    JMenu settings = new JMenu(Language.tr("menu.settings"));
    settings.setMnemonic(Language.isEnglish() ? 'S' : 'P');
    JMenuItem switchSrc = new JMenuItem("Changer de source (Mock/REST)");
    switchSrc.addActionListener(e -> switchSource());
    JMenuItem cfg = new JMenuItem("Configurer le backend (URL/Login)");
    cfg.addActionListener(e -> showBackendConfig());
    JMenuItem themeLight =
        new JMenuItem(
            new AbstractAction("Thème clair (Ctrl+Alt+L)") {
              @Override
              public void actionPerformed(ActionEvent e) {
                Theme.apply(Theme.Mode.LIGHT);
              }
            });
    JMenuItem themeDark =
        new JMenuItem(
            new AbstractAction("Thème sombre (Ctrl+Alt+D)") {
              @Override
              public void actionPerformed(ActionEvent e) {
                Theme.apply(Theme.Mode.DARK);
              }
            });
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
    JMenuItem docWysiwyg =
        new JMenuItem(
            new AbstractAction("Modèles document (WYSIWYG)") {
              @Override
              public void actionPerformed(ActionEvent e) {
                new DocTemplatesWysiwygFrame(dsp).setVisible(true);
              }
            });
    settings.add(switchSrc);
    settings.add(cfg);
    settings.add(themeLight);
    settings.add(themeDark);
    settings.addSeparator();
    settings.add(
        new JMenuItem(
            new AbstractAction(Language.tr("font.increase")) {
              @Override
              public void actionPerformed(ActionEvent e) {
                Theme.setFontScale(Theme.getFontScale() + 0.1f);
              }
            }));
    settings.add(
        new JMenuItem(
            new AbstractAction(Language.tr("font.decrease")) {
              @Override
              public void actionPerformed(ActionEvent e) {
                Theme.setFontScale(Theme.getFontScale() - 0.1f);
              }
            }));
    settings.add(
        new JMenuItem(
            new AbstractAction(Language.tr("font.reset")) {
              @Override
              public void actionPerformed(ActionEvent e) {
                Theme.setFontScale(1.0f);
              }
            }));
    JCheckBoxMenuItem highContrastItem = new JCheckBoxMenuItem(Language.tr("contrast.toggle"), Theme.isHighContrast());
    highContrastItem.addActionListener(e -> Theme.setHighContrast(highContrastItem.isSelected()));
    settings.add(highContrastItem);
    settings.addSeparator();
    settings.add(
        new JMenuItem(
            new AbstractAction(Language.tr("lang.fr")) {
              @Override
              public void actionPerformed(ActionEvent e) {
                Language.setLocale(Locale.FRENCH);
                showLanguageRestartMessage();
              }
            }));
    settings.add(
        new JMenuItem(
            new AbstractAction(Language.tr("lang.en")) {
              @Override
              public void actionPerformed(ActionEvent e) {
                Language.setLocale(Locale.ENGLISH);
                showLanguageRestartMessage();
              }
            }));
    settings.addSeparator();
    settings.add(loginItem);
    settings.add(tmpl);
    settings.add(docTmpl);
    settings.add(docHtmlTmpl);
    settings.add(docWysiwyg);
    settings.add(
        new JMenuItem(
            new AbstractAction("Types de ressources…") {
              @Override
              public void actionPerformed(ActionEvent e) {
                try {
                  new ResourceTypeManagerFrame(dsp).setVisible(true);
                } catch (RuntimeException ex) {
                  Toasts.error(MainFrame.this, "Gestion des types indisponible: " + ex.getMessage());
                }
              }
            }));

    JMenu tools = new JMenu("Outils");
    JMenuItem newIntervention = new JMenuItem("Nouvelle intervention…");
    newIntervention.addActionListener(e -> createInterventionDialog());
    JMenuItem generateData = new JMenuItem("Générer des interventions…");
    generateData.addActionListener(e -> new StressTestDialog(MainFrame.this, dsp, planning).setVisible(true));
    JMenuItem resourceColors = new JMenuItem("Couleurs des ressources…");
    resourceColors.addActionListener(e -> new ResourceColorDialog(MainFrame.this, dsp, planning).setVisible(true));
    JMenuItem conflictInspector = new JMenuItem("Alerte conflits…");
    conflictInspector.addActionListener(e -> new ConflictInspectorFrame(dsp, planning).setVisible(true));
    JMenuItem agencySettings = new JMenuItem("Paramètres agence…");
    agencySettings.addActionListener(e -> openAgencySettings());
    tools.add(newIntervention);
    tools.addSeparator();
    tools.add(generateData);
    tools.add(resourceColors);
    tools.add(conflictInspector);
    tools.add(agencySettings);

    JMenu help = new JMenu(Language.tr("menu.help"));
    help.setMnemonic(Language.isEnglish() ? 'H' : 'A');
    JMenuItem startTour = new JMenuItem("Démarrer le tour");
    startTour.addActionListener(e -> startGuidedTour());
    JMenuItem resetDemo = new JMenuItem("Réinitialiser la démo");
    resetDemo.addActionListener(e -> resetDemoData());
    JMenuItem about = new JMenuItem("À propos & fonctionnalités serveur");
    about.addActionListener(e -> showAbout());
    help.add(startTour);
    help.add(resetDemo);
    help.add(about);

    JMenu context = new JMenu("Contexte");
    context.add(agencyMenu);

    bar.add(file);
    bar.add(data);
    bar.add(documents);
    bar.add(view);
    rebuildBookmarksMenu();
    bar.add(context);
    bar.add(settings);
    bar.add(tools);
    bar.add(help);
    return bar;
  }

  private void openActivity() {
    if (activityDialog == null || activityDialog.getOwner() != this) {
      activityDialog = ActivityCenter.dialog(this);
    }
    activityDialog.setLocationRelativeTo(this);
    activityDialog.setVisible(true);
  }

  private void openAgencySettings() {
    try {
      java.util.List<Models.Agency> agencies = dsp.listAgencies();
      Models.Agency first =
          agencies.isEmpty()
              ? new Models.Agency(null, "Nouvelle agence", null, null, null)
              : agencies.get(0);
      new AgencySettingsFrame(dsp, first).setVisible(true);
    } catch (RuntimeException ex) {
      Toasts.error(this, "Paramètres agence indisponibles: " + ex.getMessage());
    }
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
    updateMinimap();
  }

  private void updateMinimap() {
    if (minimap == null) {
      return;
    }
    minimap.setWorkingHours(planning.getStartHour(), planning.getEndHour());
    minimap.setInterventions(planning.getInterventions());
    minimap.setViewportRatio(new Rectangle2D.Double(0, 0, 1, 1));
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
    if (!(dsp instanceof RestDataSource)) {
      connectionBadge.setText("🟣 Mock — source de données locale");
    }
  }

  private void updateConnectionBadge(RestDataSource rest) {
    long last = rest.getLastPingEpochMs();
    if (last <= 0L) {
      connectionBadge.setText("🟡 REST — tentative de connexion…");
      return;
    }
    long age = System.currentTimeMillis() - last;
    if (age < 20_000L) {
      connectionBadge.setText("🟢 REST connecté");
    } else if (age < 40_000L) {
      connectionBadge.setText("🟡 REST — ping en attente…");
    } else {
      connectionBadge.setText("🔴 REST déconnecté — reconnexion…");
    }
  }

  private void rebuildBookmarksMenu() {
    bookmarksMenu.removeAll();
    JMenuItem addItem = new JMenuItem("Ajouter le jour courant ⭐");
    addItem.addActionListener(e -> addCurrentDayBookmark());
    bookmarksMenu.add(addItem);
    List<String> days = prefs.getBookmarkDays();
    if (!days.isEmpty()) {
      bookmarksMenu.addSeparator();
      for (String iso : days) {
        JMenuItem dayItem = new JMenuItem(iso);
        dayItem.addActionListener(e -> openBookmarkDay(iso));
        bookmarksMenu.add(dayItem);
      }
    }
  }

  private void addCurrentDayBookmark() {
    String iso = planning.getDay().toString();
    prefs.addBookmarkDay(iso);
    prefs.save();
    rebuildBookmarksMenu();
    Toasts.info(this, "Jour ajouté aux signets");
  }

  private void openBookmarkDay(String iso) {
    try {
      LocalDate date = LocalDate.parse(iso);
      topBar.jumpTo(date);
    } catch (Exception ex) {
      Toasts.error(this, "Date invalide: " + iso);
    }
  }

  private void handleNavigation(String target) {
    switch (target) {
      case "planning" -> {
        sidebar.setSelected("planning");
        showCard("planning");
      }
      case "docs" -> {
        openDocuments();
        sidebar.setSelected("planning");
        showCard("planning");
      }
      case "clients" -> {
        if (clientsPanel == null) {
          clientsPanel = ClientsAdminFrame.createContent(dsp);
          centerCards.add(clientsPanel, "clients");
        }
        sidebar.setSelected("clients");
        showCard("clients");
      }
      case "resources" -> {
        showPlaceholder("Ressources");
        sidebar.setSelected("planning");
        showCard("planning");
      }
      case "drivers" -> {
        showPlaceholder("Chauffeurs");
        sidebar.setSelected("planning");
        showCard("planning");
      }
      case "unav" -> {
        if (unavPanel == null) {
          unavPanel = UnavailabilityFrame.createContent(dsp);
          centerCards.add(unavPanel, "unav");
        }
        sidebar.setSelected("unav");
        showCard("unav");
      }
      default -> {
        sidebar.setSelected("planning");
        showCard("planning");
      }
    }
  }

  private void registerNavigationShortcuts() {
    InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = getRootPane().getActionMap();
    for (int i = 0; i < sidebar.entryCount(); i++) {
      final int index = i;
      String actionKey = "nav-" + i;
      inputMap.put(KeyStroke.getKeyStroke("alt " + (i + 1)), actionKey);
      actionMap.put(
          actionKey,
          new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
              String target = sidebar.idAt(index);
              if (target != null) {
                handleNavigation(target);
              }
            }
          });
    }
  }

  private void startBadgeTimer() {
    if (badgeTimer != null) {
      badgeTimer.stop();
    }
    badgeTimer =
        new Timer(
            2000,
            e -> {
              sidebar.setBadge("planning", planning.conflictCount());
              try {
                List<Models.Doc> docs = dsp.listDocs(null, null);
                if (docs == null || docs.isEmpty()) {
                  sidebar.setBadgeText("docs", null);
                } else {
                  long quotes = docs.stream().filter(d -> "QUOTE".equals(d.type())).count();
                  long orders = docs.stream().filter(d -> "ORDER".equals(d.type())).count();
                  long deliveries =
                      docs.stream().filter(d -> "DELIVERY".equals(d.type())).count();
                  long invoices = docs.stream().filter(d -> "INVOICE".equals(d.type())).count();
                  long pendingDeliveries =
                      docs.stream()
                          .filter(d -> "DELIVERY".equals(d.type()) && !d.delivered())
                          .count();
                  long unpaidInvoices =
                      docs.stream()
                          .filter(d -> "INVOICE".equals(d.type()) && !d.paid())
                          .count();
                  String text =
                      "Q" + quotes +
                      " BC" + orders +
                      " BL" + deliveries +
                      " F" + invoices;
                  if (pendingDeliveries > 0 || unpaidInvoices > 0) {
                    text += " |";
                    if (pendingDeliveries > 0) {
                      text += " BL!" + pendingDeliveries;
                    }
                    if (unpaidInvoices > 0) {
                      text += " F!" + unpaidInvoices;
                    }
                  }
                  sidebar.setBadgeText("docs", text);
                }
              } catch (Exception ignored) {
              }
            });
    badgeTimer.start();
  }

  private void openCommandPalette() {
    CommandPaletteDialog dialog =
        new CommandPaletteDialog(this).commands(buildCommandPaletteCommands());
    dialog.setVisible(true);
  }

  private CommandPaletteDialog.Command[] buildCommandPaletteCommands() {
    List<CommandPaletteDialog.Command> commands = new ArrayList<>();
    commands.add(
        new CommandPaletteDialog.Command(
            "planning", "Aller au planning", () -> handleNavigation("planning")));
    commands.add(
        new CommandPaletteDialog.Command(
            "docs", "Ouvrir les documents", () -> handleNavigation("docs")));
    commands.add(
        new CommandPaletteDialog.Command(
            "new-intervention",
            "Nouvelle intervention",
            this::createInterventionDialog));
    commands.add(
        new CommandPaletteDialog.Command(
            "global-search", "Recherche globale", this::openGlobalSearch));
    commands.add(
        new CommandPaletteDialog.Command(
            "planning-today",
            "Aller à aujourd'hui",
            () -> {
              planning.setDay(LocalDate.now());
              planning.reload();
            }));
    commands.add(
        new CommandPaletteDialog.Command(
            "planning-next-conflict", "Planning: prochain conflit", planning::nextConflict));
    commands.add(
        new CommandPaletteDialog.Command(
            "planning-prev-conflict", "Planning: conflit précédent", planning::prevConflict));
    commands.add(
        new CommandPaletteDialog.Command(
            "planning-only-conflicts",
            "Planning: afficher uniquement les conflits",
            () -> planning.setFilterOnlyConflicts(true)));
    commands.add(
        new CommandPaletteDialog.Command(
            "planning-all",
            "Planning: afficher tous",
            () -> planning.setFilterOnlyConflicts(false)));
    commands.add(
        new CommandPaletteDialog.Command(
            "planning-duplicate-plus-one",
            "Dupliquer +1 jour",
            () -> planning.duplicateSelected(1)));
    commands.add(
        new CommandPaletteDialog.Command(
            "planning-duplicate-plus-seven",
            "Dupliquer +7 jours",
            () -> planning.duplicateSelected(7)));
    commands.add(
        new CommandPaletteDialog.Command(
            "export-png-view", "Export PNG (vue)", this::exportPlanningPngDialog));
    commands.add(
        new CommandPaletteDialog.Command(
            "export-png-full", "Export PNG (complet)", this::exportPlanningPdfFullDialog));
    commands.add(
        new CommandPaletteDialog.Command(
            "export-pdf-full", "Export PDF (complet)", this::exportPlanningPdfFullDialog));
    commands.add(
        new CommandPaletteDialog.Command(
            "theme-light", "Thème clair", () -> Theme.apply(Theme.Mode.LIGHT)));
    commands.add(
        new CommandPaletteDialog.Command(
            "theme-dark", "Thème sombre", () -> Theme.apply(Theme.Mode.DARK)));
    return commands.toArray(new CommandPaletteDialog.Command[0]);
  }

  private void openGlobalSearch() {
    GlobalSearchDialog dialog =
        new GlobalSearchDialog(this, dsp)
            .onOpen(
                row -> {
                  switch (row.type()) {
                    case "Document" -> {
                      handleNavigation("docs");
                      JOptionPane.showMessageDialog(
                          this,
                          "Document sélectionné : " + row.label() + " (#" + row.id() + ")",
                          "Documents",
                          JOptionPane.INFORMATION_MESSAGE);
                    }
                    case "Client" -> showPlaceholder("Client : " + row.label());
                    case "Ressource" -> showPlaceholder("Ressource : " + row.label());
                    default -> showPlaceholder(row.type() + " : " + row.label());
                  }
                });
    dialog.setVisible(true);
  }

  private void openDocuments() {
    new DocumentsBrowserFrame(dsp).setVisible(true);
  }

  private void showPlaceholder(String title) {
    JPanel placeholder = new JPanel(new BorderLayout());
    placeholder.setOpaque(false);
    EmptyState emptyState =
        new EmptyState(
            Svg.icon(Icons.SEARCH, 48),
            title,
            "Aucune donnée ici pour le moment. Utilise la barre d’outils pour créer ou filtrer.");
    placeholder.add(emptyState, BorderLayout.CENTER);
    centerCards.add(placeholder, "placeholder-" + title);
    showCard("placeholder-" + title);
  }

  private void showCard(String id) {
    java.awt.CardLayout layout = (java.awt.CardLayout) centerCards.getLayout();
    layout.show(centerCards, id);
  }

  private void exportPlanningDayCsvDialog() {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle(Language.tr("dialog.export.title"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    Path target = ensureCsvExtension(chooser.getSelectedFile());
    try {
      CsvUtil.exportPlanningDay(dsp, planning.getDay(), target);
      toastSuccess(MessageFormat.format(Language.tr("toast.export.ok"), target.getFileName()));
    } catch (IOException ex) {
      error(MessageFormat.format(Language.tr("toast.export.fail"), ex.getMessage()));
    }
  }

  private void exportPlanningDayIcsDialog() {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Export ICS");
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    Path target = ensureIcsExtension(chooser.getSelectedFile());
    try {
      IcsUtil.exportPlanningDay(dsp, planning.getDay(), target);
      toastSuccess("ICS exporté: " + target.getFileName());
    } catch (IOException ex) {
      error("Export ICS → " + ex.getMessage());
    }
  }

  private void exportPlanningPngDialog() {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Export PNG");
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    Path target = ensurePngExtension(chooser.getSelectedFile());
    try {
      ImageExport.exportComponent(planning, target);
      toastSuccess("PNG exporté: " + target.getFileName());
    } catch (IOException ex) {
      error("Export PNG → " + ex.getMessage());
    }
  }

  private void exportCsvRest() {
    if (!(dsp instanceof RestDataSource rd)) {
      error("Export CSV REST nécessite le mode REST");
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
      toastSuccess("Ressources exportées: " + tmp);
      Desktop.getDesktop().open(tmp.toFile());
    } catch (Exception ex) {
      error("Export ressources → " + ex.getMessage());
    }
  }

  private void exportClientsCsvDialog() {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle(Language.tr("dialog.export.title"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    Path target = ensureCsvExtension(chooser.getSelectedFile());
    try {
      CsvUtil.exportClients(dsp, target);
      toastSuccess(MessageFormat.format(Language.tr("toast.export.ok"), target.getFileName()));
    } catch (IOException ex) {
      error(MessageFormat.format(Language.tr("toast.export.fail"), ex.getMessage()));
    }
  }

  private void exportClientsCsvRest() {
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

  private Path ensureCsvExtension(File file) {
    Path path = file.toPath();
    String name = path.getFileName().toString();
    if (!name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
      return path.resolveSibling(name + ".csv");
    }
    return path;
  }

  private Path ensureIcsExtension(File file) {
    Path path = file.toPath();
    String name = path.getFileName().toString();
    if (!name.toLowerCase(Locale.ROOT).endsWith(".ics")) {
      return path.resolveSibling(name + ".ics");
    }
    return path;
  }

  private Path ensurePdfExtension(File file) {
    Path path = file.toPath();
    String name = path.getFileName().toString();
    if (!name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
      return path.resolveSibling(name + ".pdf");
    }
    return path;
  }

  private Path ensurePngExtension(File file) {
    Path path = file.toPath();
    String name = path.getFileName().toString();
    if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) {
      return path.resolveSibling(name + ".png");
    }
    return path;
  }

  private void showLanguageRestartMessage() {
    JOptionPane.showMessageDialog(this, Language.tr("lang.restart"));
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
      toastSuccess("PDF exporté: " + out);
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
          toastSuccess("Email envoyé (202 Accepted)");
        } else {
          toastSuccess("Email simulé (mode Mock)");
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
        toastSuccess("Intervention supprimée");
        ActivityCenter.log("Suppression intervention " + sel.id());
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
        toastSuccess("Notes enregistrées");
        refreshData();
      } catch (Exception ex) {
        error("Impossible d'enregistrer: " + ex.getMessage());
      }
    }
  }

  private void restoreWindowBounds() {
    Integer w = prefs.getWindowWidth();
    Integer h = prefs.getWindowHeight();
    if (w == null || h == null || w < 400 || h < 300) {
      setSize(1100, 720);
      setLocationRelativeTo(null);
      return;
    }
    Integer x = prefs.getWindowX();
    Integer y = prefs.getWindowY();
    restoringGeometry = true;
    if (x != null && y != null) {
      setBounds(x, y, w, h);
    } else {
      setSize(w, h);
      setLocationRelativeTo(null);
    }
    restoringGeometry = false;
  }

  private void saveGeometry() {
    if (restoringGeometry || !isShowing()) {
      return;
    }
    prefs.setWindowX(getX());
    prefs.setWindowY(getY());
    prefs.setWindowWidth(getWidth());
    prefs.setWindowHeight(getHeight());
    prefs.save();
  }

  private void resetDemoData() {
    try {
      dsp.resetDemo();
      refreshData();
      toastSuccess("Données de démonstration réinitialisées");
      ActivityCenter.log("Réinitialisation des données démo");
    } catch (RuntimeException ex) {
      error(ex.getMessage());
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
    JTextField tfTitle = new JTextField("Nouvelle intervention", 24);
    Instant start = Instant.now().plus(Duration.ofHours(1));
    JTextField tfStart = new JTextField(start.toString());
    JTextField tfEnd = new JTextField(start.plus(Duration.ofHours(2)).toString());
    JTextField tfPrice = new JTextField();
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
    panel.add(new JLabel("Prix (€):"));
    panel.add(tfPrice);
    int result = JOptionPane.showConfirmDialog(this, panel, "Créer une intervention", JOptionPane.OK_CANCEL_OPTION);
    if (result == JOptionPane.OK_OPTION) {
      Models.Resource resource = (Models.Resource) cbR.getSelectedItem();
      Models.Client client = (Models.Client) cbC.getSelectedItem();
      if (resource == null || client == null) {
        error("Sélection invalide");
        return;
      }
      try {
        Double price = null;
        String priceText = tfPrice.getText().trim();
        if (!priceText.isBlank()) {
          price = Double.parseDouble(priceText);
        }
        Models.Intervention created =
            dsp.createIntervention(
                new Models.Intervention(
                    null,
                    resource.agencyId(),
                    resource.id(),
                    client.id(),
                    null,
                    tfTitle.getText().trim(),
                    Instant.parse(tfStart.getText().trim()),
                    Instant.parse(tfEnd.getText().trim()),
                    null));
        toastSuccess("Intervention créée");
        ActivityCenter.log("Création intervention " + created.id());
        planning.rememberCreation(created, "Création");
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
        toastSuccess("Indisponibilité créée");
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
        toastSuccess("Indisponibilité récurrente créée");
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
      toastSuccess("Configuration enregistrée");
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
      toastSuccess("Envoi groupé demandé");
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
        toastSuccess("Modèle enregistré");
      } catch (Exception ex) {
        error("Échec sauvegarde : " + ex.getMessage());
      }
    }
  }

  private void toast(String message) {
    toastInfo(message);
  }

  public void toastInfo(String message) {
    status.setText(message);
    Toasts.info(this, message);
  }

  public void toastSuccess(String message) { Toasts.success(this, message); }

  private void exportPlanningPdfFullDialog() {
    if (!(dsp instanceof RestDataSource rd)) {
      error("Export PDF complet nécessite le mode REST");
      return;
    }

    ExportOptionsDialog optionsDialog = new ExportOptionsDialog(this);
    if (topBar.getFrom() != null) {
      optionsDialog.setStartDate(topBar.getFrom().toLocalDate());
    }
    optionsDialog.setVisible(true);
    if (!optionsDialog.isOk()) {
      optionsDialog.dispose();
      return;
    }
    LocalDate startDate = optionsDialog.getStartDate();
    String pageChoice = optionsDialog.getPage();
    String orientationChoice = optionsDialog.getOrientation();
    Path logoChoice = optionsDialog.getLogoPath();
    String agencyChoice = optionsDialog.getAgency();
    boolean includeRecap = optionsDialog.isRecap();
    optionsDialog.dispose();

    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Export PDF (planning complet)");
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    Path target = ensurePdfExtension(chooser.getSelectedFile());
    Path tmp = null;
    LocalDate previousDay = planning.getDay();
    try {
      planning.setDay(startDate);
      int width = Math.max(800, planning.getWidth());
      int height = planning.computeTotalHeight();
      tmp = Files.createTempFile("planning-full-", ".png");
      ImageExport.exportComponentSized(planning, width, height, tmp);
      String title =
          "Planning " + topBar.getFrom().toLocalDate() + " → " + topBar.getTo().toLocalDate();
      String page = pageChoice;
      page = page == null ? "auto" : ("Auto".equalsIgnoreCase(page) ? "auto" : page);
      String orientation = orientationChoice;
      if (orientation == null || "Auto".equalsIgnoreCase(orientation)) {
        orientation = "auto";
      } else if ("Paysage".equalsIgnoreCase(orientation)) {
        orientation = "landscape";
      } else {
        orientation = "portrait";
      }
      String period =
          topBar.getFrom().toLocalDate() + " → " + topBar.getTo().toLocalDate();
      String recapText = null;
      if (includeRecap) {
        java.util.Map<String, Integer> counts = planning.getVisibleRecapByResource();
        if (!counts.isEmpty()) {
          StringBuilder builder = new StringBuilder();
          counts.forEach((name, value) -> builder.append(name).append(": ").append(value).append('\n'));
          recapText = builder.toString();
        }
      }
      rd.uploadPlanningPngForPdf(
          tmp, title, page, orientation, logoChoice, agencyChoice, period, recapText, target);
      toastSuccess("PDF exporté: " + target.getFileName());
    } catch (Exception ex) {
      error("Export PDF complet → " + ex.getMessage());
    } finally {
      planning.setDay(previousDay);
      if (tmp != null) {
        try {
          Files.deleteIfExists(tmp);
        } catch (IOException ignore) {
          // best effort cleanup
        }
      }
    }
  }

  static class ExportOptionsDialog extends JDialog {
    private final JSpinner startSpinner =
        new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
    private final JComboBox<String> pageCombo =
        new JComboBox<>(new String[] {"Auto", "A4", "A3", "A2", "A1"});
    private final JComboBox<String> orientationCombo =
        new JComboBox<>(new String[] {"Auto", "Portrait", "Paysage"});
    private final JTextField logoField = new JTextField(24);
    private final JTextField agencyField = new JTextField(24);
    private final JCheckBox recapCheck =
        new JCheckBox("Inclure récapitulatif par ressource", true);
    private Path logoPath;
    private boolean ok;

    ExportOptionsDialog(JFrame owner) {
      super(owner, "Options d'export", true);
      setLayout(new BorderLayout(10, 10));

      JPanel form = new JPanel(new GridBagLayout());
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.anchor = GridBagConstraints.WEST;

      form.add(new JLabel("Date de début :"), constraints);
      constraints.gridx = 1;
      form.add(startSpinner, constraints);

      constraints.gridx = 0;
      constraints.gridy++;
      form.add(new JLabel("Page :"), constraints);
      constraints.gridx = 1;
      form.add(pageCombo, constraints);

      constraints.gridx = 0;
      constraints.gridy++;
      form.add(new JLabel("Orientation :"), constraints);
      constraints.gridx = 1;
      form.add(orientationCombo, constraints);

      constraints.gridx = 0;
      constraints.gridy++;
      form.add(new JLabel("Logo (PNG) :"), constraints);
      constraints.gridx = 1;

      JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
      logoPanel.add(logoField);
      JButton browseButton = new JButton("Parcourir...");
      browseButton.addActionListener(
          e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
              logoPath = chooser.getSelectedFile().toPath();
              logoField.setText(logoPath.toString());
            }
          });
      logoPanel.add(browseButton);
      form.add(logoPanel, constraints);

      constraints.gridx = 0;
      constraints.gridy++;
      form.add(new JLabel("Agence :"), constraints);
      constraints.gridx = 1;
      form.add(agencyField, constraints);

      constraints.gridx = 1;
      constraints.gridy++;
      form.add(recapCheck, constraints);

      add(form, BorderLayout.CENTER);

      JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      JButton cancelButton = new JButton("Annuler");
      JButton exportButton = new JButton("Exporter");
      cancelButton.addActionListener(
          e -> {
            ok = false;
            setVisible(false);
          });
      exportButton.addActionListener(
          e -> {
            ok = true;
            setVisible(false);
          });
      buttons.add(cancelButton);
      buttons.add(exportButton);
      add(buttons, BorderLayout.SOUTH);

      pack();
      setLocationRelativeTo(owner);
    }

    void setStartDate(LocalDate date) {
      if (date != null) {
        startSpinner.setValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
      }
    }

    boolean isOk() {
      return ok;
    }

    LocalDate getStartDate() {
      Date date = (Date) startSpinner.getValue();
      return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    String getPage() {
      return (String) pageCombo.getSelectedItem();
    }

    String getOrientation() {
      return (String) orientationCombo.getSelectedItem();
    }

    Path getLogoPath() {
      String text = logoField.getText();
      if (text != null && !text.isBlank()) {
        if (logoPath == null || !logoPath.toString().equals(text)) {
          try {
            logoPath = Path.of(text.trim());
          } catch (InvalidPathException ignored) {
            // invalid manual entry → ignore and keep previous
          }
        }
      }
      return logoPath;
    }

    String getAgency() {
      String text = agencyField.getText();
      return text == null ? null : text.trim();
    }

    boolean isRecap() {
      return recapCheck.isSelected();
    }
  }

  private void error(String message) { Toasts.error(this, message); }

  private void startGuidedTour() {
    if (!isShowing()) {
      SwingUtilities.invokeLater(this::startGuidedTour);
      return;
    }
    if (guidedTour != null) {
      getLayeredPane().remove(guidedTour);
      guidedTour = null;
    }
    GuidedTour tour =
        new GuidedTour(
            getLayeredPane(),
            () -> {
              prefs.setTourShown(true);
              prefs.save();
              guidedTour = null;
            });
    guidedTour =
        tour.addStep(
                () -> componentRect(sidebar),
                "Navigation",
                "Utilisez la barre latérale pour ouvrir planning, documents et modules à venir.")
            .addStep(
                () -> componentRect(planning),
                "Planning interactif",
                "Glissez pour déplacer ou redimensionner, double‑clic pour éditer, raccourcis Ctrl+N/D, Suppr…")
            .addStep(
                () -> componentRect(activityButton),
                "Toasts & activité",
                "Chaque action affiche un toast en bas à droite et s’enregistre dans l’activité récente.");
    guidedTour.start();
  }

  private Rectangle componentRect(java.awt.Component component) {
    if (component == null || component.getParent() == null) {
      return new Rectangle(0, 0, getWidth(), getHeight());
    }
    Rectangle rect = SwingUtilities.convertRectangle(component, ((JComponent) component).getVisibleRect(), getLayeredPane());
    if (rect.width <= 0 || rect.height <= 0) {
      rect = new Rectangle(rect.x, rect.y, Math.max(1, component.getWidth()), Math.max(1, component.getHeight()));
    }
    return rect;
  }
}
