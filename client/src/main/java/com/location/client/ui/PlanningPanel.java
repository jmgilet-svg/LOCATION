package com.location.client.ui;

import com.location.client.core.ConflictUtil;
import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import com.location.client.ui.icons.SvgIconLoader;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.FontMetrics;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.location.client.ui.uikit.Toasts;
import com.location.client.ui.uikit.Notify;

public class PlanningPanel extends JPanel {
  // --- Virtualization & caches ---
  private final java.util.Map<String, java.awt.Rectangle> tileRectCache = new java.util.HashMap<>();
  private int lastColWidth = -1;
  private java.time.OffsetDateTime lastViewFrom = null;
  private java.time.OffsetDateTime lastViewTo = null;
  private int lastWidth = -1;
  private int lastHeight = -1;
  private final java.util.Map<String, java.awt.Rectangle> chipRects = new java.util.HashMap<>();
  private final java.util.Map<String, java.awt.Rectangle> badgeBounds = new java.util.HashMap<>();
  private javax.swing.JTextField searchField;
  private String searchQuery = "";
  private final Preferences prefs =
      Preferences.userRoot().node("com.location.client.ui.accordion");
  private String animTid = null;
  private float animFactor = 1f;
  private boolean animClosing = false;
  private Timer accordionTimer;
  // S12.1: accordion mapping between display rows and resource rows
  private final java.util.List<Integer> displayToResource = new java.util.ArrayList<>(); // -1 for headers
  private final java.util.List<String> displayHeaderType = new java.util.ArrayList<>();
  private int headerRowH = 24;
  private int[] displayRowHeights = new int[0];
  private int[] displayRowYPositions = new int[0];
  private int[] resourceDisplayIndex = new int[0];

  // Ghost drag rectangle for visual feedback
  private Rectangle ghostDragRect = null;

  private final DataSourceProvider dsp;
  private List<Models.Agency> agencies = List.of();
  private List<Models.Resource> resources = List.of();
  private List<Models.ResourceType> resourceTypes = List.of();
  private final java.util.Set<String> collapsedTypes = new LinkedHashSet<>();
  private final java.util.Set<String> pinnedTypes = new LinkedHashSet<>();
  private final java.util.Map<String, String> resourceTypeIdByResource = new java.util.HashMap<>();
  private List<Models.Client> clients = List.of();
  private List<Models.Intervention> interventions = List.of();
  private List<Models.Unavailability> unavailabilities = List.of();
  private List<ConflictUtil.Conflict> conflicts = List.of();
  private final List<Runnable> reloadListeners = new ArrayList<>();
  private final java.util.Map<String, Color> resourceColors = new HashMap<>();
  private final java.util.Map<String, List<String>> interventionTags = new HashMap<>();
  private String interventionTagFilter = "";
  public interface SelectionListener {
    void onSelection(List<Models.Intervention> selection, List<Models.Intervention> dayItems);
  }

  private final List<SelectionListener> selectionListeners = new ArrayList<>();
  private final History history = History.create();
  private LocalDate day = LocalDate.now();
  private String filterAgencyId;
  private String filterResourceId;
  private String filterClientId;
  private String filterQuery = "";
  private String filterTags = "";
  private boolean filterNoConflicts;
  private boolean filterOnlyConflicts;

  private static final int BASE_HEADER_H = 28;
  private static final int CHIP_BAR_H = 24;
  private static final int ROW_H = 60;
  private static final int TIME_W = 80;
  private static final int HOURS = 12;
  private static final int START_HOUR = 7;
  private static final int SLOT_MINUTES = 15;
  private static final DayOfWeek WEEK_START = DayOfWeek.MONDAY;
  private static final Duration DEFAULT_CREATE_DURATION = Duration.ofHours(2);
  private static final Icon CONFLICT_ICON = SvgIconLoader.load("conflict.svg", 16);
  private static final String UNTYPED_TYPE_KEY = "__UNTYPED__";

  private int colWidth;
  private Tile dragTile;
  private Point dragStart;
  private boolean altDupPending = false;
  private boolean altDupDone = false;
  private boolean dragResizeLeft;
  private boolean dragResizeRight;
  private Models.Intervention selected;
  private final java.util.Map<String, Integer> laneIndexById = new java.util.HashMap<>();
  private final java.util.Map<String, Integer> laneCountByResource = new java.util.HashMap<>();
  private final java.util.Map<String, Integer> textWidthCache =
      new java.util.LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Integer> eldest) {
          return size() > 512;
        }
      };
  private int[] rowHeights = new int[0];
  private int[] rowYPositions = new int[0];
  private int hoverRow = -1;
  private String hoverTileKey;
  private boolean weekMode;
  private String flashingInterventionId;
  private double flashingPhase;
  private Timer flashingTimer;

  public PlanningPanel(DataSourceProvider dsp) {
    this.dsp = dsp;
    setLayout(null);
    searchField = new JTextField();
    searchField.setColumns(18);
    searchField.putClientProperty("JTextField.placeholderText", "Recherche ressources...");
    searchField.getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                onSearchChanged();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                onSearchChanged();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                onSearchChanged();
              }
            });
    add(searchField);
    loadAccordionPreferences();
    setBackground(Color.WHITE);
    setOpaque(true);
    reload();
    ToolTipManager.sharedInstance().registerComponent(this);
    setFocusable(true);
    setFocusTraversalKeysEnabled(false);

    getInputMap(JComponent.WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "nudgeLeft");
    getActionMap()
        .put(
            "nudgeLeft",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                nudgeTime(-SLOT_MINUTES);
              }
            });
    getInputMap(JComponent.WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nudgeRight");
    getActionMap()
        .put(
            "nudgeRight",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                nudgeTime(SLOT_MINUTES);
              }
            });
    getInputMap(JComponent.WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "nudgeUp");
    getActionMap()
        .put(
            "nudgeUp",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                nudgeTime(-SLOT_MINUTES);
              }
            });
    getInputMap(JComponent.WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "nudgeDown");
    getActionMap()
        .put(
            "nudgeDown",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                nudgeTime(SLOT_MINUTES);
              }
            });
    getInputMap(JComponent.WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK), "resourceUp");
    getActionMap()
        .put(
            "resourceUp",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                nudgeResource(-1);
              }
            });
    getInputMap(JComponent.WHEN_FOCUSED)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK), "resourceDown");
    getActionMap()
        .put(
            "resourceDown",
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent e) {
                nudgeResource(1);
              }
            });

    registerConflictNavigationShortcuts();

    MouseAdapter adapter = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        altDupPending = e.isAltDown();
        altDupDone = false;
        onPress(e.getPoint());
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        onDrag(e.getPoint());
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        onRelease();
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        updateCursor(e.getPoint());
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        if (SwingUtilities.isRightMouseButton(e)) {
          String headerType = headerTypeAtPoint(p);
          if (headerType != null) {
            showHeaderMenu(e, headerType);
            return;
          }
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
          String headerType = headerTypeAtPoint(p);
          if (headerType != null && isHeaderCollapsible(headerType)) {
            toggleTypeCollapse(headerType);
            return;
          }
          for (java.util.Map.Entry<String, java.awt.Rectangle> entry : new java.util.HashMap<>(chipRects).entrySet()) {
            java.awt.Rectangle rect = entry.getValue();
            if (rect != null && rect.contains(p)) {
              String typeId = entry.getKey();
              if (typeId != null && isHeaderCollapsible(typeId)) {
                toggleTypeCollapse(typeId);
                return;
              }
            }
          }
        }
        Models.Intervention badgeTarget = interventionAtBadge(p);
        if (badgeTarget != null) {
          showResourcePopover(e, badgeTarget);
          return;
        }
        if (SwingUtilities.isRightMouseButton(e)) {
          findTileAt(p).map(t -> t.i).ifPresent(it -> showResourcePopover(e, it));
          return;
        }
        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
          Optional<Tile> hit = findTileAt(p);
          if (hit.isPresent()) {
            openQuickEdit(hit.get().i);
          } else {
            createAt(p);
          }
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        hoverRow = -1;
        hoverTileKey = null;
        repaint();
        setCursor(Cursor.getDefaultCursor());
      }
    };
    addMouseListener(adapter);
    addMouseMotionListener(adapter);
    registerUndoShortcuts();
  }

  private void onSearchChanged() {
    if (searchField == null) {
      return;
    }
    String text = searchField.getText();
    if (text == null) {
      text = "";
    }
    String normalized = text.trim().toLowerCase(Locale.ROOT);
    if (Objects.equals(normalized, searchQuery)) {
      return;
    }
    searchQuery = normalized;
    reload();
    repaint();
  }

  private void loadAccordionPreferences() {
    collapsedTypes.clear();
    pinnedTypes.clear();
    String collapsedPref = prefs.get("collapsedTypes", "");
    if (collapsedPref != null && !collapsedPref.isBlank()) {
      for (String token : collapsedPref.split(",")) {
        String trimmed = token.trim();
        if (!trimmed.isEmpty()) {
          collapsedTypes.add(trimmed);
        }
      }
    }
    String pinnedPref = prefs.get("pinnedTypes", "");
    if (pinnedPref != null && !pinnedPref.isBlank()) {
      for (String token : pinnedPref.split(",")) {
        String trimmed = token.trim();
        if (!trimmed.isEmpty()) {
          pinnedTypes.add(trimmed);
        }
      }
    }
  }

  private void saveCollapsedTypes() {
    prefs.put("collapsedTypes", String.join(",", collapsedTypes));
  }

  private void savePinnedTypes() {
    prefs.put("pinnedTypes", String.join(",", pinnedTypes));
  }

  private void toggleTypeCollapse(String typeId) {
    if (!isHeaderCollapsible(typeId)) {
      return;
    }
    boolean nowCollapsed;
    if (collapsedTypes.contains(typeId)) {
      collapsedTypes.remove(typeId);
      nowCollapsed = false;
    } else {
      collapsedTypes.add(typeId);
      nowCollapsed = true;
    }
    saveCollapsedTypes();
    startAccordionAnim(typeId, nowCollapsed);
  }

  private void startAccordionAnim(String typeId, boolean closing) {
    if (accordionTimer != null && accordionTimer.isRunning()) {
      accordionTimer.stop();
    }
    animTid = typeId;
    animClosing = closing;
    animFactor = closing ? 1f : 0f;
    reload();
    repaint();
    final long start = System.currentTimeMillis();
    final int durationMs = 120;
    accordionTimer =
        new Timer(
            15,
            evt -> {
              long elapsed = System.currentTimeMillis() - start;
              float progress = Math.min(1f, Math.max(0f, elapsed / (float) durationMs));
              animFactor = closing ? 1f - progress : progress;
              if (progress >= 1f) {
                ((Timer) evt.getSource()).stop();
                accordionTimer = null;
                animTid = null;
                animFactor = 1f;
                animClosing = false;
              }
              repaint();
            });
    accordionTimer.setRepeats(true);
    accordionTimer.start();
  }

  private String headerTypeAtPoint(Point p) {
    int displayIndex = displayRowAtY(p.y);
    if (displayIndex < 0 || displayIndex >= displayToResource.size()) {
      return null;
    }
    Integer mapped = displayToResource.get(displayIndex);
    if (mapped != null && mapped >= 0) {
      return null;
    }
    if (displayIndex >= displayHeaderType.size()) {
      return null;
    }
    return displayHeaderType.get(displayIndex);
  }

  private void showHeaderMenu(MouseEvent e, String typeId) {
    if (typeId == null) {
      return;
    }
    JPopupMenu menu = new JPopupMenu();
    JMenuItem pinItem =
        new JMenuItem(pinnedTypes.contains(typeId) ? "Désépingler" : "Épingler en haut");
    pinItem.addActionListener(
        ev -> {
          if (pinnedTypes.contains(typeId)) {
            pinnedTypes.remove(typeId);
          } else {
            pinnedTypes.add(typeId);
          }
          savePinnedTypes();
          reload();
          repaint();
        });
    menu.add(pinItem);
    if (isHeaderCollapsible(typeId)) {
      boolean collapsed = collapsedTypes.contains(typeId);
      JMenuItem toggleItem = new JMenuItem(collapsed ? "Développer" : "Réduire");
      toggleItem.addActionListener(ev -> toggleTypeCollapse(typeId));
      menu.add(toggleItem);
    }
    menu.show(this, e.getX(), e.getY());
  }

  @Override
  public void doLayout() {
    super.doLayout();
    if (searchField != null) {
      int prefHeight = searchField.getPreferredSize().height;
      int available = Math.max(140, getWidth() - (TIME_W + 32));
      int width = Math.min(280, available);
      int x = TIME_W + 16;
      if (x + width > getWidth() - 8) {
        width = Math.max(120, getWidth() - x - 8);
      }
      int headerZone = chipBarHeight();
      if (headerZone <= 0) {
        headerZone = BASE_HEADER_H;
      }
      int y = Math.max(4, (headerZone - prefHeight) / 2);
      searchField.setBounds(x, y, width, prefHeight);
    }
  }

  private void createAt(Point p) {
    if (p.y < headerHeight() || resources.isEmpty() || clients.isEmpty()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    if (findTileAt(p).isPresent()) {
      return;
    }
    int row = rowAtY(p.y);
    if (row < 0 || row >= resources.size()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    Models.Resource resource = resources.get(row);
    Instant start = alignToSlot(instantForX(p.x));
    JComboBox<Models.Client> clientCombo = new JComboBox<>(clients.toArray(new Models.Client[0]));
    clientCombo.setRenderer(
        new DefaultListCellRenderer() {
          @Override
          public java.awt.Component getListCellRendererComponent(
              javax.swing.JList<?> list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Models.Client client) {
              setText(client.name());
            }
            return this;
          }
        });
    JTextField titleField = new JTextField("Nouvelle intervention", 20);
    JTextField driverField = new JTextField(15);
    JSpinner durationSpinner =
        new JSpinner(
            new SpinnerNumberModel(
                (int) DEFAULT_CREATE_DURATION.toMinutes(),
                SLOT_MINUTES,
                HOURS * 60,
                SLOT_MINUTES));

    ZonedDateTime zonedStart = start.atZone(ZoneId.systemDefault());
    JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
    panel.add(new JLabel("Ressource:"));
    panel.add(new JLabel(resource.name()));
    panel.add(new JLabel("Date:"));
    panel.add(new JLabel(zonedStart.toLocalDate().toString()));
    panel.add(new JLabel("Début:"));
    panel.add(new JLabel(zonedStart.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString()));
    panel.add(new JLabel("Durée (min):"));
    panel.add(durationSpinner);
    panel.add(new JLabel("Client:"));
    panel.add(clientCombo);
    panel.add(new JLabel("Chauffeur (id):"));
    panel.add(driverField);
    panel.add(new JLabel("Titre:"));
    panel.add(titleField);

    int option =
        JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            panel,
            "Créer une intervention",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
    if (option != JOptionPane.OK_OPTION) {
      return;
    }
    Models.Client client = (Models.Client) clientCombo.getSelectedItem();
    if (client == null) {
      Toolkit.getDefaultToolkit().beep();
      JOptionPane.showMessageDialog(this, "Sélectionnez un client valide.");
      return;
    }
    String title = titleField.getText().isBlank() ? "Nouvelle intervention" : titleField.getText().trim();
    String driverId = driverField.getText().isBlank() ? null : driverField.getText().trim();
    int minutes = ((Number) durationSpinner.getValue()).intValue();
    Instant end = start.plus(Duration.ofMinutes(minutes));
    Models.Intervention payload =
        new Models.Intervention(
            null,
            resource.agencyId(),
            resource.id(),
            client.id(),
            driverId,
            title,
            start,
            end,
            null);
    try {
      ensureAvailability(resource.id(), start, end);
      Models.Intervention created = dsp.createIntervention(payload);
      selected = created;
      reload();
      notifySuccess("Intervention créée", "Création intervention " + created.id());
      maybeOfferQuote(created);
      pushCreationHistory("Création", created);
    } catch (RuntimeException ex) {
      Toolkit.getDefaultToolkit().beep();
      JOptionPane.showMessageDialog(
          this,
          "Impossible de créer l'intervention: " + ex.getMessage(),
          "Erreur",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void openQuickEdit(Models.Intervention intervention) {
    List<Models.Resource> allResources = dsp.listResources();
    QuickEditDialog dialog =
        new QuickEditDialog(
                SwingUtilities.getWindowAncestor(this), dsp, intervention, allResources)
            .onSavedPair(
                (original, saved) -> {
                  selected = saved;
                  reload();
                  pushUpdateHistory("Édition", copyOf(original), saved);
                });
    dialog.setVisible(true);
  }

  public void setWeekMode(boolean week) {
    if (this.weekMode == week) {
      return;
    }
    this.weekMode = week;
    reload();
    repaint();
  }

  public boolean isWeekMode() {
    return weekMode;
  }

  private int getViewDays() {
    return weekMode ? 7 : 1;
  }

  private LocalDate getViewStart() {
    if (weekMode) {
      return day.with(TemporalAdjusters.previousOrSame(WEEK_START));
    }
    return day;
  }

  public OffsetDateTime getViewFrom() {
    LocalDate start = getViewStart();
    return start.atTime(0, 0).atOffset(ZoneOffset.UTC);
  }

  public OffsetDateTime getViewTo() {
    LocalDate start = getViewStart();
    return start.plusDays(getViewDays()).atTime(0, 0).atOffset(ZoneOffset.UTC);
  }

  private int chipBarHeight() {
    if (resourceTypes == null || resourceTypes.isEmpty()) {
      return 0;
    }
    return CHIP_BAR_H;
  }

  private int headerHeight() {
    return BASE_HEADER_H + chipBarHeight();
  }

  public void reload() {
    invalidateLayoutCaches();
    String selectedId = getSelectedInterventionId();
    agencies = dsp.listAgencies();
    List<Models.Resource> fetchedResources = dsp.listResources();
    String agency = filterAgencyId;
    if (agency != null && !agency.isBlank()) {
      fetchedResources =
          fetchedResources.stream().filter(r -> agency.equals(r.agencyId())).toList();
    }
    if (filterTags != null && !filterTags.isBlank()) {
      Set<String> requested =
          Arrays.stream(filterTags.toLowerCase().split("\\s*,\\s*")).filter(s -> !s.isBlank()).collect(Collectors.toSet());
      if (!requested.isEmpty()) {
        fetchedResources =
            fetchedResources.stream()
                .filter(
                    r ->
                        r.tags() != null
                            && requested.stream()
                                .allMatch(t -> r.tags().toLowerCase().contains(t)))
                .toList();
      }
    }
    if (filterResourceId != null && !filterResourceId.isBlank()) {
      String rid = filterResourceId;
      fetchedResources = fetchedResources.stream().filter(r -> rid.equals(r.id())).toList();
    }
    if (searchQuery != null && !searchQuery.isBlank()) {
      String needle = searchQuery;
      fetchedResources =
          fetchedResources.stream()
              .filter(
                  r -> {
                    if (r == null) {
                      return false;
                    }
                    String name = r.name();
                    return name != null && name.toLowerCase(Locale.ROOT).contains(needle);
                  })
              .toList();
    }
    List<Models.ResourceType> fetchedTypes;
    try {
      fetchedTypes = dsp.listResourceTypes();
    } catch (RuntimeException ex) {
      fetchedTypes = List.of();
    }
    if (fetchedTypes == null) {
      fetchedTypes = List.of();
    }
    resourceTypes = fetchedTypes;
    java.util.Set<String> knownTypeIds =
        resourceTypes.stream()
            .map(Models.ResourceType::id)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    knownTypeIds.add(UNTYPED_TYPE_KEY);
    collapsedTypes.retainAll(knownTypeIds);
    saveCollapsedTypes();
    pinnedTypes.retainAll(knownTypeIds);
    savePinnedTypes();

    resourceTypeIdByResource.clear();
    for (Models.Resource resource : fetchedResources) {
      if (resource == null) {
        continue;
      }
      String id = resource.id();
      if (id == null) {
        continue;
      }
      String typeId = null;
      try {
        typeId = dsp.getResourceTypeForResource(id);
      } catch (RuntimeException ignored) {
        typeId = null;
      }
      resourceTypeIdByResource.put(id, typeId);
    }

    if (filterResourceId != null && !filterResourceId.isBlank()) {
      String forcedType = resourceTypeIdByResource.get(filterResourceId);
      if (forcedType != null) {
        if (collapsedTypes.remove(forcedType)) {
          saveCollapsedTypes();
        }
      }
    }

    java.util.Map<String, Integer> typeOrder = new java.util.HashMap<>();
    for (int i = 0; i < resourceTypes.size(); i++) {
      Models.ResourceType type = resourceTypes.get(i);
      if (type != null && type.id() != null) {
        typeOrder.put(type.id(), i);
      }
    }

    java.text.Collator collator = java.text.Collator.getInstance();
    List<Models.Resource> sortedResources =
        fetchedResources.stream()
            .filter(
                r -> {
                  if (r == null || r.id() == null) {
                    return true;
                  }
                  String typeId = resourceTypeIdByResource.get(r.id());
                  return typeId == null || !collapsedTypes.contains(typeId);
                })
            .sorted(
                (a, b) -> {
                  if (a == null || b == null) {
                    return a == null ? (b == null ? 0 : -1) : 1;
                  }
                  String ta = a.id() == null ? null : resourceTypeIdByResource.get(a.id());
                  String tb = b.id() == null ? null : resourceTypeIdByResource.get(b.id());
                  int oa = typeOrder.getOrDefault(ta, Integer.MAX_VALUE / 2);
                  int ob = typeOrder.getOrDefault(tb, Integer.MAX_VALUE / 2);
                  if (oa != ob) {
                    return Integer.compare(oa, ob);
                  }
                  String an = a.name() == null ? "" : a.name();
                  String bn = b.name() == null ? "" : b.name();
                  return collator.compare(an, bn);
                })
            .toList();

    resources = sortedResources;
    rebuildDisplayIndex();
    rebuildResourceColors();

    clients = dsp.listClients();

    OffsetDateTime from = getViewFrom();
    OffsetDateTime to = getViewTo();
    List<Models.Intervention> data = dsp.listInterventions(from, to, normalize(filterResourceId));
    if (agency != null && !agency.isBlank()) {
      data = data.stream().filter(i -> agency.equals(i.agencyId())).toList();
    }
    if (filterClientId != null && !filterClientId.isBlank()) {
      String cid = filterClientId;
      data = data.stream().filter(i -> cid.equals(i.clientId())).toList();
    }
    if (filterQuery != null && !filterQuery.isBlank()) {
      String q = filterQuery.toLowerCase();
      data =
          data.stream()
              .filter(i -> i.title() != null && i.title().toLowerCase().contains(q))
              .toList();
    }
    loadInterventionTags(data);
    if (interventionTagFilter != null && !interventionTagFilter.isBlank()) {
      data = data.stream().filter(this::matchesInterventionTagFilter).toList();
    }
    if (filterTags != null && !filterTags.isBlank()) {
      Set<String> visibleIds = resources.stream().map(Models.Resource::id).collect(Collectors.toSet());
      data = data.stream().filter(i -> visibleIds.contains(i.resourceId())).toList();
    }
    List<ConflictUtil.Conflict> computedConflicts = ConflictUtil.computeConflicts(data);
    if (filterOnlyConflicts && !computedConflicts.isEmpty()) {
      Set<String> conflictIds =
          computedConflicts.stream()
              .flatMap(c -> Stream.of(c.a().id(), c.b().id()))
              .filter(id -> id != null && !id.isBlank())
              .collect(Collectors.toSet());
      data =
          data.stream()
              .filter(i -> i.id() != null && conflictIds.contains(i.id()))
              .toList();
      computedConflicts = ConflictUtil.computeConflicts(data);
    }
    if (filterNoConflicts && !computedConflicts.isEmpty()) {
      Set<String> conflictIds =
          computedConflicts.stream()
              .flatMap(c -> Stream.of(c.a().id(), c.b().id()))
              .filter(id -> id != null && !id.isBlank())
              .collect(Collectors.toSet());
      data =
          data.stream()
              .filter(i -> i.id() == null || !conflictIds.contains(i.id()))
              .toList();
      computedConflicts = ConflictUtil.computeConflicts(data);
    }
    retainInterventionTagsFor(data);
    conflicts = computedConflicts;
    try {
      Notify.post("conflicts.update", conflicts);
    } catch (Throwable ignore) {
    }
    interventions = data;
    if (selectedId != null) {
      selected =
          interventions.stream()
              .filter(i -> selectedId.equals(i.id()))
              .findFirst()
              .orElse(null);
    } else {
      selected = null;
    }

    List<Models.Unavailability> unav =
        dsp.listUnavailabilities(from, to, normalize(filterResourceId));
    Set<String> visibleResourceIds =
        resources.stream().map(Models.Resource::id).collect(Collectors.toSet());
    unavailabilities =
        unav.stream().filter(u -> visibleResourceIds.contains(u.resourceId())).toList();
    computeDynamicRows();
    notifyReloadListeners();
    fireSelectionChanged();
    repaint();
  }

  public void addReloadListener(Runnable listener) {
    if (listener != null) {
      reloadListeners.add(listener);
    }
  }

  public void addSelectionListener(SelectionListener listener) {
    if (listener != null) {
      selectionListeners.add(listener);
      try {
        listener.onSelection(
            selected == null ? List.of() : List.of(selected), List.copyOf(interventions));
      } catch (RuntimeException ignored) {
      }
    }
  }

  private void notifyReloadListeners() {
    if (reloadListeners.isEmpty()) {
      return;
    }
    for (Runnable listener : new ArrayList<>(reloadListeners)) {
      try {
        listener.run();
      } catch (RuntimeException ex) {
        // On ignore les erreurs des listeners pour ne pas casser le rafraîchissement principal.
      }
    }
  }

  public int computeTotalHeight() {
    ensureRowLayout();
    if (displayToResource.isEmpty()) {
      return headerHeight() + headerRowH;
    }
    int total = headerHeight();
    for (int idx = 0; idx < displayToResource.size(); idx++) {
      total += displayRowH(idx);
    }
    return total;
  }

  private void rebuildResourceColors() {
    resourceColors.clear();
    for (Models.Resource resource : resources) {
      if (resource.id() != null) {
        resourceColors.put(resource.id(), ResourceColors.colorFor(resource));
      }
    }
  }

  private void rebuildDisplayIndex() {
    displayToResource.clear();
    displayHeaderType.clear();
    int count = resources == null ? 0 : resources.size();
    resourceDisplayIndex = new int[count];
    java.util.Arrays.fill(resourceDisplayIndex, -1);
    if ((resourceTypes == null || resourceTypes.isEmpty()) && count <= 0) {
      displayRowHeights = new int[0];
      displayRowYPositions = new int[0];
      return;
    }

    java.util.Map<String, java.util.List<Integer>> byType = new java.util.LinkedHashMap<>();
    for (int i = 0; i < count; i++) {
      Models.Resource resource = resources.get(i);
      if (resource == null || resource.id() == null) {
        continue;
      }
      String typeId = resourceTypeIdByResource.get(resource.id());
      if (typeId == null) {
        typeId = UNTYPED_TYPE_KEY;
      }
      byType.computeIfAbsent(typeId, key -> new java.util.ArrayList<>()).add(i);
    }

    java.util.LinkedHashSet<String> ordered = new java.util.LinkedHashSet<>();
    if (resourceTypes != null) {
      for (Models.ResourceType type : resourceTypes) {
        if (type != null && type.id() != null) {
          ordered.add(type.id());
        }
      }
    }
    for (String typeId : byType.keySet()) {
      if (!ordered.contains(typeId)) {
        ordered.add(typeId);
      }
    }
    if (byType.containsKey(UNTYPED_TYPE_KEY)) {
      ordered.add(UNTYPED_TYPE_KEY);
    }

    if (ordered.isEmpty() && count > 0) {
      ordered.add(UNTYPED_TYPE_KEY);
    }

    java.util.List<String> orderedList = new java.util.ArrayList<>(ordered);
    java.util.List<String> head = new java.util.ArrayList<>();
    java.util.List<String> tail = new java.util.ArrayList<>();
    for (String typeId : orderedList) {
      if (pinnedTypes.contains(typeId)) {
        head.add(typeId);
      } else {
        tail.add(typeId);
      }
    }
    java.util.List<String> finalOrder = new java.util.ArrayList<>(head);
    finalOrder.addAll(tail);

    for (String typeId : finalOrder) {
      java.util.List<Integer> indexes = byType.getOrDefault(typeId, java.util.List.of());
      if (indexes.isEmpty() && totalResourcesForType(typeId) <= 0) {
        continue;
      }
      displayToResource.add(-1);
      displayHeaderType.add(typeId);
      for (Integer idx : indexes) {
        if (idx == null || idx < 0 || idx >= count) {
          continue;
        }
        displayToResource.add(idx);
        displayHeaderType.add(null);
        resourceDisplayIndex[idx] = displayToResource.size() - 1;
      }
    }

    displayRowHeights = new int[displayToResource.size()];
    displayRowYPositions = new int[displayToResource.size()];
  }

  private void invalidateLayoutCaches() {
    tileRectCache.clear();
    lastColWidth = -1;
    lastViewFrom = null;
    lastViewTo = null;
    lastWidth = -1;
    lastHeight = -1;
  }

  private void updateLayoutCacheState(
      int width,
      int height,
      int columnWidth,
      java.time.OffsetDateTime viewFrom,
      java.time.OffsetDateTime viewTo) {
    if (lastWidth != width
        || lastHeight != height
        || lastColWidth != columnWidth
        || !Objects.equals(lastViewFrom, viewFrom)
        || !Objects.equals(lastViewTo, viewTo)) {
      tileRectCache.clear();
      lastWidth = width;
      lastHeight = height;
      lastColWidth = columnWidth;
      lastViewFrom = viewFrom;
      lastViewTo = viewTo;
    }
  }

  private void fireSelectionChanged() {
    if (selectionListeners.isEmpty()) {
      return;
    }
    List<Models.Intervention> snapshot = List.copyOf(interventions);
    List<Models.Intervention> selection = selected == null ? List.of() : List.of(selected);
    for (SelectionListener listener : new ArrayList<>(selectionListeners)) {
      try {
        listener.onSelection(selection, snapshot);
      } catch (RuntimeException ignored) {
      }
    }
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public void setTagFilter(String value) {
    String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    if (Objects.equals(normalized, interventionTagFilter)) {
      return;
    }
    interventionTagFilter = normalized;
    reload();
    repaint();
  }

  public String getTagFilter() {
    return interventionTagFilter;
  }

  private void loadInterventionTags(List<Models.Intervention> items) {
    interventionTags.clear();
    if (items == null || items.isEmpty()) {
      return;
    }
    for (Models.Intervention intervention : items) {
      if (intervention == null) {
        continue;
      }
      String id = intervention.id();
      if (id == null || id.isBlank()) {
        continue;
      }
      try {
        List<String> tags = dsp.getInterventionTags(id);
        interventionTags.put(id, tags == null ? List.of() : List.copyOf(tags));
      } catch (RuntimeException ignored) {
        // ignore les échecs réseau pour ne pas bloquer le rechargement
      }
    }
  }

  private void retainInterventionTagsFor(List<Models.Intervention> items) {
    if (items == null) {
      interventionTags.clear();
      return;
    }
    Set<String> ids =
        items.stream()
            .map(Models.Intervention::id)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    interventionTags.keySet().removeIf(id -> !ids.contains(id));
  }

  private boolean matchesInterventionTagFilter(Models.Intervention intervention) {
    if (intervention == null) {
      return false;
    }
    if (interventionTagFilter == null || interventionTagFilter.isBlank()) {
      return true;
    }
    String id = intervention.id();
    if (id == null || id.isBlank()) {
      return false;
    }
    List<String> tags = interventionTags.get(id);
    if (tags == null || tags.isEmpty()) {
      return false;
    }
    String needle = interventionTagFilter;
    for (String tag : tags) {
      if (tag != null && tag.toLowerCase(Locale.ROOT).contains(needle)) {
        return true;
      }
    }
    return false;
  }

  public void setDay(LocalDate value) {
    if (value == null || value.equals(day)) {
      return;
    }
    day = value;
    reload();
    repaint();
  }

  public LocalDate getDay() {
    return day;
  }

  public int getStartHour() {
    return START_HOUR;
  }

  public int getEndHour() {
    return START_HOUR + HOURS;
  }

  public void setFilterAgency(String value) {
    filterAgencyId = normalize(value);
    reload();
    repaint();
  }

  public void setFilterResource(String value) {
    filterResourceId = normalize(value);
    reload();
    repaint();
  }

  public void setFilterClient(String value) {
    filterClientId = normalize(value);
    reload();
    repaint();
  }

  public void setFilterQuery(String value) {
    filterQuery = value == null ? "" : value;
    reload();
    repaint();
  }

  public void setFilterTags(String value) {
    filterTags = value == null ? "" : value;
    reload();
    repaint();
  }

  public void setFilterNoConflicts(boolean value) {
    if (filterNoConflicts == value) {
      return;
    }
    filterNoConflicts = value;
    reload();
    repaint();
  }

  public String getFilterAgencyId() {
    return filterAgencyId;
  }

  public String getFilterResourceId() {
    return filterResourceId;
  }

  public String getFilterClientId() {
    return filterClientId;
  }

  public String getFilterQuery() {
    return filterQuery;
  }

  public String getFilterTags() {
    return filterTags;
  }

  public boolean isFilterOnlyConflicts() {
    return filterOnlyConflicts;
  }

  public void setFilterOnlyConflicts(boolean value) {
    if (filterOnlyConflicts == value) {
      return;
    }
    filterOnlyConflicts = value;
    reload();
    repaint();
  }

  public boolean isFilterNoConflicts() {
    return filterNoConflicts;
  }

  public List<Models.Resource> getResources() {
    return resources;
  }

  public void refreshResourceColors() {
    rebuildResourceColors();
    repaint();
  }

  public List<Models.Agency> getAgencies() {
    return agencies;
  }

  public List<Models.Client> getClients() {
    return clients;
  }

  public List<Models.Intervention> getInterventions() {
    return interventions;
  }

  public List<Models.Unavailability> getUnavailabilities() {
    return unavailabilities;
  }

  public List<ConflictUtil.Conflict> getConflicts() {
    return List.copyOf(conflicts);
  }

  public int conflictCount() {
    return conflicts == null ? 0 : conflicts.size();
  }

  public Models.Intervention getSelected() {
    return selected;
  }

  public String getSelectedInterventionId() {
    return selected == null ? null : selected.id();
  }

  public boolean hasSelection() {
    return selected != null;
  }

  public boolean duplicateSelected() {
    return duplicateSelectedWithDelta(Duration.ofHours(1), " (copie)");
  }

  public boolean duplicateSelected(int days) {
    Duration delta = Duration.ofDays(days);
    String suffix = days == 0 ? " (copie)" : " (copie +" + days + "j)";
    return duplicateSelectedWithDelta(delta, suffix);
  }

  private boolean duplicateSelectedWithDelta(Duration delta, String suffix) {
    if (selected == null) {
      Toolkit.getDefaultToolkit().beep();
      return false;
    }
    Models.Intervention base = selected;
    Instant start = base.start().plus(delta);
    Instant end = base.end().plus(delta);
    if (!end.isAfter(start)) {
      Toolkit.getDefaultToolkit().beep();
      return false;
    }
    String title = base.title();
    if (title == null || title.isBlank()) {
      title = "Intervention";
    }
    if (suffix != null && !suffix.isBlank()) {
      title += suffix;
    }
    Models.Intervention payload =
        new Models.Intervention(
            null,
            base.agencyId(),
            base.resourceId(),
            base.clientId(),
            base.driverId(),
            title,
            start,
            end,
            base.notes());
    try {
      ensureAvailability(base.resourceId(), start, end);
      Models.Intervention created = dsp.createIntervention(payload);
      selected = created;
      reload();
      notifySuccess("Intervention dupliquée", "Duplication intervention " + created.id());
      pushCreationHistory("Duplication", created);
      return true;
    } catch (RuntimeException ex) {
      Toolkit.getDefaultToolkit().beep();
      JOptionPane.showMessageDialog(
          this,
          "Impossible de dupliquer l'intervention: " + ex.getMessage(),
          "Erreur",
          JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }

  public boolean shiftSelection(Duration delta) {
    if (selected == null) {
      Toolkit.getDefaultToolkit().beep();
      return false;
    }
    Models.Intervention before = copyOf(selected);
    Instant start = selected.start().plus(delta);
    Instant end = selected.end().plus(delta);
    if (!end.isAfter(start)) {
      Toolkit.getDefaultToolkit().beep();
      return false;
    }
    Models.Intervention updated =
        new Models.Intervention(
            selected.id(),
            selected.agencyId(),
            selected.resourceId(),
            selected.clientId(),
            selected.driverId(),
            selected.title(),
            start,
            end,
            selected.notes());
    try {
      ensureAvailability(selected.resourceId(), start, end);
      Models.Intervention persisted = dsp.updateIntervention(updated);
      selected = persisted;
      reload();
      notifySuccess("Intervention décalée", "Déplacement intervention " + persisted.id());
      pushUpdateHistory("Déplacement", before, persisted);
      return true;
    } catch (RuntimeException ex) {
      Toolkit.getDefaultToolkit().beep();
      JOptionPane.showMessageDialog(
          this,
          "Décalage impossible: " + ex.getMessage(),
          "Erreur",
          JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }

  public void undoLast() {
    String label = history.undo();
    if (label == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    ActivityCenter.log("Undo: " + label);
    showHistoryToast("Annulé", label);
  }

  public void redoLast() {
    String label = history.redo();
    if (label == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    ActivityCenter.log("Redo: " + label);
    showHistoryToast("Rétabli", label);
  }

  public void clearSelection() {
    if (selected != null) {
      selected = null;
      repaint();
      fireSelectionChanged();
    }
  }

  private void computeLanes(java.time.Instant from, java.time.Instant to) {
    laneIndexById.clear();
    laneCountByResource.clear();
    java.util.Map<String, java.util.List<Models.Intervention>> byResource = new java.util.HashMap<>();
    for (Models.Intervention intervention : interventions) {
      if (intervention == null || intervention.resourceId() == null) {
        continue;
      }
      if (intervention.end().isAfter(from) && intervention.start().isBefore(to)) {
        byResource
            .computeIfAbsent(intervention.resourceId(), key -> new java.util.ArrayList<>())
            .add(intervention);
      }
    }
    for (java.util.Map.Entry<String, java.util.List<Models.Intervention>> entry : byResource.entrySet()) {
      String resourceId = entry.getKey();
      java.util.List<Models.Intervention> list = entry.getValue();
      list.sort(java.util.Comparator.comparing(Models.Intervention::start));
      java.util.List<java.time.Instant> laneEnds = new java.util.ArrayList<>();
      for (Models.Intervention intervention : list) {
        int laneIndex = -1;
        for (int idx = 0; idx < laneEnds.size(); idx++) {
          if (!intervention.start().isBefore(laneEnds.get(idx))) {
            laneIndex = idx;
            break;
          }
        }
        if (laneIndex < 0) {
          laneIndex = laneEnds.size();
          laneEnds.add(intervention.end());
        } else {
          java.time.Instant currentEnd = laneEnds.get(laneIndex);
          if (intervention.end().isAfter(currentEnd)) {
            laneEnds.set(laneIndex, intervention.end());
          }
        }
        if (intervention.id() != null) {
          laneIndexById.put(intervention.id(), laneIndex);
        }
      }
      laneCountByResource.put(resourceId, Math.max(1, laneEnds.size()));
    }
    computeDynamicRows();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int w = getWidth();
    int h = getHeight();
    java.awt.Rectangle vis = getVisibleRect();
    if (vis.width <= 0 || vis.height <= 0) {
      return;
    }
    int headerH = headerHeight();
    int chipBar = chipBarHeight();
    java.time.OffsetDateTime viewFrom = getViewFrom();
    java.time.OffsetDateTime viewTo = getViewTo();
    int viewDays = Math.max(1, getViewDays());
    int totalCols = Math.max(1, HOURS * viewDays);
    int availableWidth = Math.max(0, w - TIME_W);
    colWidth = Math.max(1, availableWidth / totalCols);
    updateLayoutCacheState(w, h, colWidth, viewFrom, viewTo);
    int dayWidth = colWidth * HOURS;

    computeLanes(viewFrom.toInstant(), viewTo.toInstant());
    ensureRowLayout();
    badgeBounds.clear();
    chipRects.clear();

    int displayCount = displayToResource.size();
    int firstVisibleDisplayRow = 0;
    int lastVisibleDisplayRow = displayCount - 1;
    if (displayCount == 0) {
      lastVisibleDisplayRow = -1;
    } else {
      int bodyTop = headerH;
      int bodyBottom = displayRowY(displayCount - 1) + displayRowH(displayCount - 1);
      int viewStartY = Math.max(bodyTop, vis.y);
      int viewEndY = Math.min(bodyBottom, vis.y + vis.height);
      if (viewEndY <= bodyTop || viewStartY >= bodyBottom) {
        firstVisibleDisplayRow = 0;
        lastVisibleDisplayRow = -1;
      } else {
        int firstIdx = displayRowAtY(viewStartY);
        if (firstIdx < 0) {
          firstIdx = 0;
        }
        int lastPixel = Math.max(viewStartY, viewEndY - 1);
        int lastIdx = displayRowAtY(lastPixel);
        if (lastIdx < 0) {
          lastIdx = displayCount - 1;
        }
        firstVisibleDisplayRow = Math.max(0, firstIdx);
        lastVisibleDisplayRow =
            Math.min(displayCount - 1, Math.max(firstVisibleDisplayRow, lastIdx));
      }
    }

    g2.setColor(new Color(250, 250, 255));
    g2.fillRect(0, 0, TIME_W, h);
    g2.setColor(new Color(245, 245, 245));
    if (hoverRow >= 0 && isResourceVisibleInRect(hoverRow, vis)) {
      int hy = rowY(hoverRow);
      int hh = rowH(hoverRow);
      g2.setColor(new Color(100, 149, 237, 28));
      g2.fillRect(TIME_W, hy, Math.max(0, w - TIME_W), hh);
      g2.setColor(new Color(245, 245, 245));
    }
    if (!displayToResource.isEmpty()) {
      g2.setColor(new Color(230, 230, 230));
      int bodyWidth = Math.max(0, w - TIME_W);
      for (int idx = 0; idx < displayToResource.size(); idx++) {
        int yRow = displayRowY(idx);
        g2.drawLine(TIME_W, yRow, TIME_W + bodyWidth, yRow);
      }
      int bottom = displayRowY(displayToResource.size() - 1) + displayRowH(displayToResource.size() - 1);
      g2.drawLine(TIME_W, bottom, TIME_W + bodyWidth, bottom);
      g2.setColor(new Color(245, 245, 245));
    }
    g2.fillRect(TIME_W, 0, Math.max(0, w - TIME_W), headerH);
    g2.setColor(Color.GRAY);
    g2.drawLine(0, headerH, w, headerH);
    g2.drawLine(TIME_W, 0, TIME_W, h);

    if (chipBar > 0) {
      g2.setColor(new Color(242, 247, 255));
      g2.fillRect(TIME_W, 0, Math.max(0, w - TIME_W), chipBar);
      java.awt.FontMetrics fm = g2.getFontMetrics();
      int chipX = TIME_W + 8;
      int chipY = Math.max(2, (chipBar - (fm.getHeight() + 6)) / 2);
      int chipHeight = Math.min(chipBar - chipY * 2, fm.getHeight() + 6);
      if (chipHeight < fm.getHeight()) {
        chipHeight = fm.getHeight() + 4;
      }
      for (Models.ResourceType type : resourceTypes) {
        if (type == null || type.id() == null) {
          continue;
        }
        long total =
            resourceTypeIdByResource.entrySet().stream()
                .filter(e -> type.id().equals(e.getValue()))
                .count();
        if (total <= 0) {
          continue;
        }
        long visible =
            resources.stream()
                .filter(r -> type.id().equals(resourceTypeIdByResource.get(r.id())))
                .count();
        boolean collapsed = collapsedTypes.contains(type.id());
        String baseLabel = type.label() == null || type.label().isBlank() ? type.code() : type.label();
        if (baseLabel == null || baseLabel.isBlank()) {
          baseLabel = type.id();
        }
        String label = baseLabel + " (" + visible + "/" + total + ")";
        String text = (collapsed ? "\u25B6 " : "\u25BC ") + label;
        int textWidth = fm.stringWidth(text);
        int chipWidth = Math.min(Math.max(64, textWidth + 16), Math.max(64, w - chipX - 8));
        java.awt.Rectangle rect = new java.awt.Rectangle(chipX, chipY, chipWidth, chipHeight);
        chipRects.put(type.id(), rect);
        g2.setColor(collapsed ? new Color(228, 232, 240) : new Color(204, 221, 255));
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 12, 12);
        g2.setColor(collapsed ? new Color(150, 160, 180) : new Color(130, 150, 190));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 12, 12);
        g2.setColor(Color.DARK_GRAY);
        int textY = rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, rect.x + 8, textY);
        chipX += rect.width + 8;
        if (chipX > w - 80) {
          break;
        }
      }
    }

    paintHeatmap(g2, viewDays, dayWidth, h);

    LocalDate headerStart = getViewStart();
    int dayLabelY = (chipBar > 0 ? chipBar + 16 : 16);
    for (int d = 0; d < viewDays; d++) {
      int xDay = TIME_W + d * dayWidth;
      g2.setColor(new Color(230, 230, 230));
      g2.drawLine(xDay, 0, xDay, h);
      g2.setColor(Color.DARK_GRAY);
      LocalDate current = headerStart.plusDays(d);
      String label =
          current.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault())
              + " "
              + current;
      g2.drawString(label, xDay + 6, dayLabelY);
      g2.setColor(Color.GRAY);
      for (int i = 0; i <= HOURS; i++) {
        int x = xDay + i * colWidth;
        g2.drawLine(x, headerH, x, h);
        if (d == 0 && i < HOURS) {
          String txt = (START_HOUR + i) + ":00";
          g2.drawString(txt, x + 4, headerH - 8);
        }
      }
    }
    int boundaryX = TIME_W + viewDays * dayWidth;
    g2.drawLine(boundaryX, 0, boundaryX, h);

    g2.setColor(Color.GRAY);
    if (lastVisibleDisplayRow >= firstVisibleDisplayRow && lastVisibleDisplayRow >= 0) {
      for (int idx = firstVisibleDisplayRow; idx <= lastVisibleDisplayRow; idx++) {
        if (idx < 0 || idx >= displayCount) {
          continue;
        }
        int top = displayRowY(idx);
        int height = displayRowH(idx);
        Integer mapped = displayToResource.get(idx);
        if (mapped == null || mapped < 0) {
          g2.setColor(new Color(240, 244, 252));
          g2.fillRect(0, top, w, height);
          g2.setColor(new Color(200, 208, 224));
          g2.drawLine(0, top, w, top);
          g2.drawLine(0, top + height, w, top + height);
          g2.setColor(new Color(70, 80, 100));
          String typeId = displayHeaderType.size() > idx ? displayHeaderType.get(idx) : null;
          String label = headerLabel(typeId);
          java.awt.FontMetrics fm = g2.getFontMetrics();
          int textY = top + (height + fm.getAscent() - fm.getDescent()) / 2;
          g2.drawString(label, 8, textY);
          g2.setColor(Color.GRAY);
        } else {
          g2.drawLine(0, top, w, top);
          g2.setColor(Color.DARK_GRAY);
          Models.Resource resource = resources.get(mapped);
          String label = resource == null ? "" : resource.name();
          if (label != null && !label.isBlank()) {
            int textBaseline = top + Math.min(height - 6, 18);
            g2.drawString(label, 8, textBaseline);
          }
          g2.setColor(Color.GRAY);
        }
      }
    }
    if (!displayToResource.isEmpty()) {
      int bottom =
          displayRowY(displayToResource.size() - 1) + displayRowH(displayToResource.size() - 1);
      g2.drawLine(0, bottom, w, bottom);
    }

    for (Models.Unavailability unav : unavailabilities) {
      int row = indexOfResource(unav.resourceId());
      if (!isResourceVisibleInRect(row, vis)) {
        continue;
      }
      int x1 = xForInstant(unav.start());
      int x2 = xForInstant(unav.end());
      int y = rowY(row) + 6;
      int height = rowH(row) - 12;
      paintHatched(g2, Math.min(x1, x2), y, Math.max(12, Math.abs(x2 - x1)), height, unav.recurring());
    }

    for (Models.Intervention i : interventions) {
      int r = indexOfResource(i.resourceId());
      if (!isResourceVisibleInRect(r, vis)) {
        continue;
      }
      Tile t = tileFor(i, r);
      paintTile(g2, t);
    }

    g2.setFont(getFont());
    for (Models.Intervention i : interventions) {
      if (i.notes() == null || i.notes().isBlank()) {
        continue;
      }
      int r = indexOfResource(i.resourceId());
      if (!isResourceVisibleInRect(r, vis)) {
        continue;
      }
      Tile t = tileFor(i, r);
      int iconX = Math.max(t.x1, t.x2) - 18;
      int iconY = rowY(r) + 18;
      g2.setColor(new Color(30, 30, 30, 200));
      g2.drawString("\uD83D\uDCD3", iconX, iconY);
    }

    if (selected != null) {
      int row = indexOfResource(selected.resourceId());
      if (isResourceVisibleInRect(row, vis)) {
        Tile t = tileFor(selected, row);
        int x = Math.min(t.x1, t.x2);
        int w1 = Math.max(16, Math.abs(t.x2 - t.x1));
        int y = rowY(row) + 4;
        int height = rowH(row) - 8;
        Stroke old = g2.getStroke();
        g2.setColor(new Color(255, 200, 0, 180));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(x - 4, y, w1 + 8, height, 14, 14);
        g2.setStroke(old);
      }
    }

    if (dragTile != null && isResourceVisibleInRect(dragTile.row, vis)) {
      paintTile(g2, dragTile.withAlpha(0.6f));
    }

    if (interventions != null && conflicts != null && !conflicts.isEmpty()) {
      Graphics2D g2pulse = (Graphics2D) g2.create();
      long now = System.currentTimeMillis() % 1200L;
      float phase = Math.abs(600 - now) / 600f;
      float alpha = 0.3f + 0.5f * phase;
      g2pulse.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
      g2pulse.setColor(new Color(220, 50, 47));
      g2pulse.setStroke(new BasicStroke(2.5f));
      for (Models.Intervention intervention : interventions) {
        if (!isInConflict(intervention.id())) {
          continue;
        }
        int row = indexOfResource(intervention.resourceId());
        if (!isResourceVisibleInRect(row, vis)) {
          continue;
        }
        java.awt.Rectangle rect = tileRect(tileFor(intervention, row));
        if (rect == null) {
          continue;
        }
        java.awt.Rectangle outline = new java.awt.Rectangle(rect);
        outline.grow(4, 4);
        g2pulse.drawRoundRect(outline.x, outline.y, outline.width, outline.height, 14, 14);
      }
      g2pulse.dispose();
    }

    if (ghostDragRect != null) {
      Graphics2D ghostGraphics = (Graphics2D) g2.create();
      ghostGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
      ghostGraphics.setColor(new Color(0, 120, 215));
      ghostGraphics.fill(ghostDragRect);
      ghostGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
      ghostGraphics.setStroke(new BasicStroke(2f));
      ghostGraphics.draw(ghostDragRect);
      ghostGraphics.dispose();
    }
  }

  private void paintHeatmap(Graphics2D g2, int viewDays, int dayWidth, int height) {
    if (interventions.isEmpty() || dayWidth <= 0) {
      return;
    }
    int stepsPerDay = HOURS * 2;
    int bodyHeight = Math.max(0, height - headerHeight());
    if (bodyHeight <= 0) {
      return;
    }
    ZoneId zone = ZoneId.systemDefault();
    LocalDate startDate = getViewStart();
    for (int dayIndex = 0; dayIndex < viewDays; dayIndex++) {
      LocalDate currentDay = startDate.plusDays(dayIndex);
      for (int step = 0; step < stepsPerDay; step++) {
        java.time.ZonedDateTime slotStartZdt =
            currentDay.atTime(START_HOUR, 0).atZone(zone).plusMinutes(30L * step);
        Instant slotStart = slotStartZdt.toInstant();
        Instant slotEnd = slotStartZdt.plusMinutes(30).toInstant();
        int count = 0;
        for (Models.Intervention intervention : interventions) {
          if (intervention.end().isAfter(slotStart)
              && intervention.start().isBefore(slotEnd)) {
            count++;
          }
        }
        if (count <= 0) {
          continue;
        }
        int alpha = Math.min(90, 18 * count + 10);
        int x1 =
            TIME_W
                + dayIndex * dayWidth
                + Math.round(dayWidth * (step / (float) stepsPerDay));
        int x2 =
            TIME_W
                + dayIndex * dayWidth
                + Math.round(dayWidth * ((step + 1) / (float) stepsPerDay));
        g2.setColor(new Color(255, 140, 0, alpha));
        g2.fillRect(x1, headerHeight(), Math.max(1, x2 - x1), bodyHeight);
      }
    }
  }

  private int indexOfResource(String id) {
    for (int i = 0; i < resources.size(); i++) {
      if (resources.get(i).id().equals(id)) return i;
    }
    return -1;
  }

  private Tile tileFor(Models.Intervention i, int row) {
    int x1 = xForInstant(i.start());
    int x2 = xForInstant(i.end());
    return new Tile(i, row, x1, x2);
  }

  private int xForInstant(Instant instant) {
    if (colWidth <= 0) {
      return TIME_W;
    }
    int viewDays = Math.max(1, getViewDays());
    LocalDate startDate = getViewStart();
    LocalDate endDate = startDate.plusDays(viewDays);
    ZonedDateTime zoned = instant.atZone(ZoneId.systemDefault());
    LocalDateTime ldt = zoned.toLocalDateTime();
    LocalDate date = ldt.toLocalDate();
    if (date.isBefore(startDate)) {
      ldt = LocalDateTime.of(startDate, LocalTime.of(START_HOUR, 0));
    } else if (!date.isBefore(endDate)) {
      ldt = LocalDateTime.of(endDate.minusDays(1), LocalTime.of(START_HOUR, 0));
    }
    int dayOffset =
        (int)
            Math.max(
                0,
                Math.min(viewDays - 1, ChronoUnit.DAYS.between(startDate, ldt.toLocalDate())));
    LocalTime time = ldt.toLocalTime();
    int mins = (time.getHour() - START_HOUR) * 60 + time.getMinute();
    mins = Math.max(0, Math.min(HOURS * 60, mins));
    int base = TIME_W + dayOffset * HOURS * colWidth;
    return base + Math.round((mins / 60f) * colWidth);
  }

  private Instant instantForX(int x) {
    if (colWidth <= 0) {
      return getViewFrom().toInstant();
    }
    int viewDays = Math.max(1, getViewDays());
    int totalCols = HOURS * viewDays;
    int max = totalCols * colWidth;
    int rel = Math.max(0, Math.min(x - TIME_W, max));
    float slots = rel / (float) colWidth;
    int totalMins = Math.round(slots * 60f);
    int minutesPerDay = HOURS * 60;
    int dayOffset = Math.min(viewDays - 1, Math.max(0, totalMins / minutesPerDay));
    int minsInDay = totalMins - dayOffset * minutesPerDay;
    minsInDay = Math.max(0, Math.min(minutesPerDay, minsInDay));
    LocalDate targetDay = getViewStart().plusDays(dayOffset);
    LocalDateTime ldt =
        LocalDateTime.of(targetDay, LocalTime.of(START_HOUR, 0)).plusMinutes(minsInDay);
    return ldt.atZone(ZoneId.systemDefault()).toInstant();
  }

  private Instant alignToSlot(Instant instant) {
    ZonedDateTime zoned = instant.atZone(ZoneId.systemDefault()).withSecond(0).withNano(0);
    int minute = zoned.getMinute();
    int remainder = minute % SLOT_MINUTES;
    if (remainder != 0) {
      zoned = zoned.minusMinutes(remainder);
    }
    return zoned.toInstant();
  }

  private Models.Intervention pickNextConflict(boolean backward) {
    if (conflicts == null || conflicts.isEmpty()) {
      return null;
    }
    java.util.LinkedHashSet<Models.Intervention> unique = new java.util.LinkedHashSet<>();
    for (ConflictUtil.Conflict conflict : conflicts) {
      if (conflict.a() != null) {
        unique.add(conflict.a());
      }
      if (conflict.b() != null) {
        unique.add(conflict.b());
      }
    }
    java.util.List<Models.Intervention> ordered = new java.util.ArrayList<>(unique);
    ordered.sort(java.util.Comparator.comparing(Models.Intervention::start));
    java.time.Instant pivot =
        selected != null ? selected.start() : getViewFrom().toInstant();
    if (backward) {
      Models.Intervention candidate = null;
      for (int i = ordered.size() - 1; i >= 0; i--) {
        Models.Intervention intervention = ordered.get(i);
        if (intervention.start().isBefore(pivot)) {
          candidate = intervention;
          break;
        }
      }
      if (candidate == null && !ordered.isEmpty()) {
        candidate = ordered.get(ordered.size() - 1);
      }
      return candidate;
    }
    for (Models.Intervention intervention : ordered) {
      if (intervention.start().isAfter(pivot)) {
        return intervention;
      }
    }
    return ordered.isEmpty() ? null : ordered.get(0);
  }

  private void centerOn(Models.Intervention intervention) {
    if (intervention == null) {
      return;
    }
    java.time.ZonedDateTime zoned =
        intervention.start().atZone(java.time.ZoneId.systemDefault());
    setDay(zoned.toLocalDate());
    selected = intervention;
    if (intervention.id() != null) {
      triggerConflictFlash(intervention.id());
    }
    repaint();
  }

  private void triggerConflictFlash(String interventionId) {
    if (interventionId == null) {
      return;
    }
    flashingInterventionId = interventionId;
    flashingPhase = 0d;
    if (flashingTimer != null && flashingTimer.isRunning()) {
      flashingTimer.stop();
    }
    flashingTimer =
        new Timer(
            70,
            e -> {
              flashingPhase += 0.4d;
              if (flashingPhase > Math.PI * 4) {
                flashingTimer.stop();
                flashingInterventionId = null;
              }
              repaint();
            });
    flashingTimer.setRepeats(true);
    flashingTimer.start();
  }

  private void paintTile(Graphics2D g2, Tile t) {
    java.awt.Rectangle rect = tileRect(t);
    int x = rect.x;
    int y = rect.y;
    int w = rect.width;
    int height = rect.height;

    boolean conflict = hasConflict(t);
    Color base = resourceColors.get(t.i.resourceId());
    if (base == null) {
      base = ResourceColors.colorFor(null);
    }
    if (t.alpha < 1f) {
      g2.setComposite(AlphaComposite.SrcOver.derive(t.alpha));
    }
    g2.setColor(base);
    g2.fillRoundRect(x, y, w, height, 10, 10);
    if (conflict) {
      g2.setColor(new Color(219, 68, 55, 70));
      g2.fillRoundRect(x, y, w, height, 10, 10);
    }
    g2.setColor(conflict ? new Color(219, 68, 55, 200) : new Color(0, 0, 0, 80));
    g2.drawRoundRect(x, y, w, height, 10, 10);
    if (hoverTileKey != null && hoverTileKey.equals(tileKey(t))) {
      paintHoverGlow(g2, x, y, w, height);
    }
    g2.setComposite(AlphaComposite.SrcOver);

    if (conflict) {
      Icon warn = CONFLICT_ICON;
      if (warn != null) {
        int iconWidth = warn.getIconWidth();
        int iconX = Math.max(x + 4, x + w - iconWidth - 6);
        int iconY = y + 4;
        warn.paintIcon(this, g2, iconX, iconY);
      } else {
        g2.setColor(new Color(219, 68, 55, 220));
        g2.fillOval(x + w - 14, y + 6, 10, 10);
      }
    }

    g2.setColor(new Color(255, 255, 255, 160));
    g2.fillRect(x, y, 4, height);
    g2.fillRect(x + w - 4, y, 4, height);

    if (flashingInterventionId != null && flashingInterventionId.equals(t.i.id())) {
      float pulse = (float) (0.5 + 0.5 * Math.sin(flashingPhase));
      int alpha = Math.min(255, (int) (120 + 120 * pulse));
      g2.setColor(new Color(219, 68, 55, alpha));
      g2.fillRoundRect(x, y, w, height, 10, 10);
    }

    g2.setColor(Color.WHITE);
    g2.setFont(getFont().deriveFont(Font.BOLD));
    drawWrapped(g2, t.i.title(), x + 8, y + 4, Math.max(8, w - 16), Math.max(12, height - 16));

    paintTileBadges(g2, t, rect);
  }

  private void paintTileBadges(Graphics2D g2, Tile tile, java.awt.Rectangle rect) {
    if (tile == null || tile.i == null || rect == null) {
      return;
    }

    java.util.List<String> resourceIds = effectiveResourceIds(tile.i);
    String interventionId = tile.i.id();
    if (interventionId != null && resourceIds.size() <= 1) {
      badgeBounds.remove(interventionId);
    }
    if (resourceIds.size() > 1) {
      Graphics2D badgeGraphics = (Graphics2D) g2.create();
      try {
        float baseSize = getFont() != null ? getFont().getSize2D() : 12f;
        badgeGraphics.setFont(getFont().deriveFont(Font.BOLD, Math.max(10f, baseSize - 1f)));
        String label = "+" + (resourceIds.size() - 1);
        java.awt.FontMetrics fm = badgeGraphics.getFontMetrics();
        int badgeWidth = fm.stringWidth(label) + 8;
        int badgeHeight = fm.getHeight();
        int badgeX = rect.x + rect.width - badgeWidth - 4;
        int badgeY = rect.y + 4;
        badgeGraphics.setColor(new Color(0, 0, 0, 100));
        badgeGraphics.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 8, 8);
        badgeGraphics.setColor(Color.WHITE);
        badgeGraphics.drawString(label, badgeX + 4, badgeY + fm.getAscent());
        if (interventionId != null) {
          badgeBounds.put(interventionId, new java.awt.Rectangle(badgeX, badgeY, badgeWidth, badgeHeight));
        }
      } finally {
        badgeGraphics.dispose();
      }
    }

    if (isInConflict(tile.i.id())) {
      g2.setColor(new Color(200, 50, 50));
      int dotX = rect.x - 6;
      int dotY = rect.y - 6;
      g2.fillOval(dotX, dotY, 10, 10);
    }
  }

  private Models.Intervention interventionAtBadge(Point p) {
    if (p == null || badgeBounds.isEmpty() || interventions == null || interventions.isEmpty()) {
      return null;
    }
    for (java.util.Map.Entry<String, java.awt.Rectangle> entry : badgeBounds.entrySet()) {
      java.awt.Rectangle bounds = entry.getValue();
      if (bounds != null && bounds.contains(p)) {
        String id = entry.getKey();
        if (id == null) {
          continue;
        }
        for (Models.Intervention intervention : interventions) {
          if (id.equals(intervention.id())) {
            return intervention;
          }
        }
      }
    }
    return null;
  }

  private java.util.List<String> effectiveResourceIds(Models.Intervention intervention) {
    if (intervention == null) {
      return java.util.List.of();
    }
    java.util.List<String> ids = intervention.resourceIds();
    if (ids != null && !ids.isEmpty()) {
      return ids;
    }
    String legacyId = intervention.resourceId();
    return (legacyId == null || legacyId.isBlank()) ? java.util.List.of() : java.util.List.of(legacyId);
  }

  private java.awt.Rectangle tileRect(Tile t) {
    if (t == null) {
      return new java.awt.Rectangle();
    }
    String resourceId = t.i.resourceId();
    int laneCount = Math.max(1, laneCountByResource.getOrDefault(resourceId, 1));
    int laneIndex = 0;
    String interventionId = t.i.id();
    if (interventionId != null) {
      laneIndex = laneIndexById.getOrDefault(interventionId, 0);
    }
    laneIndex = Math.max(0, Math.min(laneCount - 1, laneIndex));
    if (t.alpha != 1f) {
      return buildTileRect(t, laneIndex, laneCount);
    }
    String key = tileKey(t);
    if (key != null) {
      String cacheKey = key + "|" + t.row + "|" + laneIndex + "|" + laneCount;
      java.awt.Rectangle cached = tileRectCache.get(cacheKey);
      if (cached != null) {
        return cached;
      }
      java.awt.Rectangle rect = buildTileRect(t, laneIndex, laneCount);
      tileRectCache.put(cacheKey, rect);
      return rect;
    }
    return buildTileRect(t, laneIndex, laneCount);
  }

  private java.awt.Rectangle buildTileRect(Tile t, int laneIndex, int laneCount) {
    int rowHeight = rowH(t.row);
    int baseY = rowY(t.row) + 6;
    int baseHeight = Math.max(16, rowHeight - 12);
    int x = Math.min(t.x1, t.x2);
    int w = Math.max(16, Math.abs(t.x2 - t.x1));
    int innerHeight = Math.max(18, baseHeight / Math.max(1, laneCount));
    int y = baseY + laneIndex * innerHeight;
    int h = Math.max(16, innerHeight - 2);
    return new java.awt.Rectangle(x, y, w, h);
  }

  private void paintHatched(Graphics2D g2, int x, int y, int width, int height, boolean recurring) {
    int alpha = recurring ? 35 : 60;
    int strokeAlpha = recurring ? 90 : 140;
    Color base = new Color(219, 68, 55, alpha);
    g2.setColor(base);
    g2.fillRoundRect(x, y, width, height, 10, 10);
    g2.setColor(new Color(219, 68, 55, strokeAlpha));
    for (int i = x - height; i < x + width; i += 8) {
      g2.drawLine(i, y, i + height, y + height);
    }
    g2.setColor(new Color(180, 0, 0, recurring ? 80 : 120));
    g2.drawRoundRect(x, y, width, height, 10, 10);
  }

  private boolean hasConflict(Tile t) {
    if (t == null || t.i == null) {
      return false;
    }
    String resourceId = t.i.resourceId();
    String id = t.i.id();
    if (!conflicts.isEmpty() && id != null) {
      for (ConflictUtil.Conflict conflict : conflicts) {
        boolean sameIntervention =
            id.equals(conflict.a().id()) || id.equals(conflict.b().id());
        boolean sameResource =
            conflict.resourceId() == null || Objects.equals(conflict.resourceId(), resourceId);
        if (sameIntervention && sameResource) {
          return true;
        }
      }
    }
    if (resourceId == null) {
      return false;
    }
    Instant s = instantForX(Math.min(t.x1, t.x2));
    Instant e = instantForX(Math.max(t.x1, t.x2));
    for (Models.Intervention intervention : interventions) {
      if (Objects.equals(intervention.id(), id)) {
        continue;
      }
      if (!intervention.resourceIds().contains(resourceId)) {
        continue;
      }
      if (intervention.end().isAfter(s) && intervention.start().isBefore(e)) {
        return true;
      }
    }
    return false;
  }

  private Optional<Tile> findTileAt(Point p) {
    if (p == null || p.y < headerHeight() || p.x < TIME_W) {
      return Optional.empty();
    }
    int row = rowAtY(p.y);
    if (row < 0 || row >= resources.size()) {
      return Optional.empty();
    }
    Models.Resource resource = resources.get(row);
    if (resource == null) {
      return Optional.empty();
    }
    String resourceId = resource.id();
    for (Models.Intervention i : interventions) {
      if (!Objects.equals(i.resourceId(), resourceId)) {
        continue;
      }
      Tile t = tileFor(i, row);
      Rectangle rect = tileRect(t);
      if (rect.contains(p)) return Optional.of(t);
    }
    return Optional.empty();
  }

  private void showResourcePopover(MouseEvent event, Models.Intervention target) {
    if (event == null || target == null) {
      return;
    }
    java.util.List<String> current = new java.util.ArrayList<>(effectiveResourceIds(target));
    if (current.isEmpty()) {
      String fallback = target.resourceId();
      if (fallback != null && !fallback.isBlank()) {
        current.add(fallback);
      }
    }

    JPopupMenu menu = new JPopupMenu();

    if (!current.isEmpty()) {
      JMenu assigned = new JMenu("Ressources affectées");
      for (String rid : current) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(rid, true);
        item.addActionListener(
            e -> {
              if (current.size() <= 1) {
                Toolkit.getDefaultToolkit().beep();
                return;
              }
              java.util.List<String> ids = new java.util.ArrayList<>(effectiveResourceIds(target));
              if (ids.isEmpty()) {
                ids = new java.util.ArrayList<>(current);
              }
              ids.remove(rid);
              if (ids.isEmpty()) {
                Toolkit.getDefaultToolkit().beep();
                return;
              }
              tryUpdateResources(target, ids);
            });
        assigned.add(item);
      }
      menu.add(assigned);
    }

    JMenu candidates = new JMenu("Ajouter une ressource");
    for (Models.Resource resource : resources) {
      if (resource == null) {
        continue;
      }
      String rid = resource.id();
      if (rid == null || rid.isBlank() || current.contains(rid)) {
        continue;
      }
      JMenuItem item = new JMenuItem(resource.name() + " (" + rid + ")");
      item.addActionListener(
          e -> {
            java.util.List<String> ids = new java.util.ArrayList<>(effectiveResourceIds(target));
            if (ids.isEmpty()) {
              ids = new java.util.ArrayList<>(current);
            }
            if (!ids.contains(rid)) {
              ids.add(rid);
            }
            tryUpdateResources(target, ids);
          });
      candidates.add(item);
    }
    if (candidates.getItemCount() == 0) {
      JMenuItem none = new JMenuItem("Aucune ressource disponible");
      none.setEnabled(false);
      candidates.add(none);
    }
    menu.add(candidates);

    int row = rowAtY(event.getY());
    if (row >= 0 && row < resources.size()) {
      Models.Resource rowResource = resources.get(row);
      if (rowResource != null && rowResource.id() != null && !rowResource.id().isBlank()) {
        JMenuItem keepOnly = new JMenuItem("Ne garder que cette ressource (rangée)");
        keepOnly.addActionListener(
            e -> {
              java.util.List<String> ids = new java.util.ArrayList<>();
              ids.add(rowResource.id());
              tryUpdateResources(target, ids);
            });
        menu.addSeparator();
        menu.add(keepOnly);
      }
    }

    menu.show(this, event.getX(), event.getY());
  }

  private void tryUpdateResources(Models.Intervention original, java.util.List<String> resourceIds) {
    if (original == null || resourceIds == null) {
      return;
    }
    java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
    for (String rid : resourceIds) {
      if (rid != null && !rid.isBlank()) {
        unique.add(rid);
      }
    }
    if (unique.isEmpty()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    java.util.List<String> sanitized = new java.util.ArrayList<>(unique);
    try {
      Models.Intervention updated = original.withResourceIds(sanitized);
      Models.Intervention saved = dsp.updateIntervention(updated);
      selected = saved;
      reload();
      if (saved.id() != null) {
        selectAndRevealIntervention(saved.id());
      }
      notifySuccess("Affectations mises à jour", "Ressources: " + String.join(", ", sanitized));
      pushUpdateHistory("Affectations", original, saved);
    } catch (RuntimeException ex) {
      notifyError("Impossible de mettre à jour les affectations");
    }
  }

  public boolean deleteSelected() {
    if (selected == null) {
      return false;
    }
    try {
      Models.Intervention removed = copyOf(selected);
      dsp.deleteIntervention(selected.id());
      selected = null;
      reload();
      pushDeletionHistory("Suppression", removed);
      return true;
    } catch (RuntimeException ex) {
      JOptionPane.showMessageDialog(
          this, "Suppression impossible: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }

  public void rememberCreation(Models.Intervention created) {
    rememberCreation(created, "Création");
  }

  public void rememberCreation(Models.Intervention created, String label) {
    if (created == null) {
      return;
    }
    String effective = (label == null || label.isBlank()) ? "Création" : label;
    pushCreationHistory(effective, created);
  }

  private void nudgeTime(int minutes) {
    if (selected == null) {
      return;
    }
    Models.Intervention before = copyOf(selected);
    int resourceIndex = indexOfResource(selected.resourceId());
    if (resourceIndex < 0) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    Instant newStart = selected.start().plus(Duration.ofMinutes(minutes));
    Instant newEnd = selected.end().plus(Duration.ofMinutes(minutes));
    if (!newEnd.isAfter(newStart)) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    Models.Resource resource = resources.get(resourceIndex);
    Models.Intervention updated =
        new Models.Intervention(
            selected.id(),
            resource.agencyId(),
            resource.id(),
            selected.clientId(),
            selected.driverId(),
            selected.title(),
            newStart,
            newEnd,
            selected.notes());
    try {
      ensureAvailability(resource.id(), newStart, newEnd);
      Models.Intervention persisted = dsp.updateIntervention(updated);
      selected = persisted;
      reload();
      pushUpdateHistory("Déplacement", before, persisted);
    } catch (RuntimeException ex) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  private void nudgeResource(int delta) {
    if (selected == null || resources.isEmpty()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    Models.Intervention before = copyOf(selected);
    int current = indexOfResource(selected.resourceId());
    if (current < 0) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    int target = Math.max(0, Math.min(resources.size() - 1, current + delta));
    if (target == current) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    Models.Resource resource = resources.get(target);
    Models.Intervention updated =
        new Models.Intervention(
            selected.id(),
            resource.agencyId(),
            resource.id(),
            selected.clientId(),
            selected.driverId(),
            selected.title(),
            selected.start(),
            selected.end(),
            selected.notes());
    try {
      ensureAvailability(resource.id(), selected.start(), selected.end());
      Models.Intervention persisted = dsp.updateIntervention(updated);
      selected = persisted;
      reload();
      pushUpdateHistory("Changement ressource", before, persisted);
    } catch (RuntimeException ex) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  private void updateCursor(Point p) {
    int newHoverRow = rowAtY(p.y);
    if (p.y < headerHeight() || p.x < TIME_W || newHoverRow < 0 || newHoverRow >= resources.size()) {
      newHoverRow = -1;
    }
    Optional<Tile> otHover = findTileAt(p);
    String newHoverTileKey = otHover.map(this::tileKey).orElse(null);
    if (newHoverRow != hoverRow || !Objects.equals(newHoverTileKey, hoverTileKey)) {
      hoverRow = newHoverRow;
      hoverTileKey = newHoverTileKey;
      repaint();
    }

    Optional<Tile> ot = otHover;
    if (ot.isEmpty()) {
      setCursor(Cursor.getDefaultCursor());
      return;
    }
    Tile t = ot.get();
    int x = Math.min(t.x1, t.x2);
    int w = Math.max(16, Math.abs(t.x2 - t.x1));
    if (Math.abs(p.x - x) <= 5 || Math.abs(p.x - (x + w)) <= 5) {
      setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
    } else {
      setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }
  }

  private void onPress(Point p) {
    requestFocusInWindow();
    Optional<Tile> ot = findTileAt(p);
    dragStart = p;
    dragResizeLeft = false;
    dragResizeRight = false;
    if (ot.isPresent()) {
      Tile tile = ot.get();
      selected = tile.i;
      dragTile = tile;
      repaint();
      fireSelectionChanged();
      int x = Math.min(tile.x1, tile.x2);
      int w = Math.max(16, Math.abs(tile.x2 - tile.x1));
      if (Math.abs(p.x - x) <= 5) {
        dragResizeLeft = true;
      } else if (Math.abs(p.x - (x + w)) <= 5) {
        dragResizeRight = true;
      }
    } else {
      dragTile = null;
      if (selected != null) {
        selected = null;
        repaint();
        fireSelectionChanged();
      }
    }
  }

  private void onDrag(Point p) {
    if (altDupPending && !altDupDone) {
      if (selected != null) {
        boolean ok = duplicateSelected();
        altDupDone = ok;
        if (ok) {
          findTileAt(p).ifPresent(t -> dragTile = t);
        }
      }
    }
    if (dragTile == null || dragStart == null) {
      return;
    }
    int dx = p.x - dragStart.x;
    Tile tile = dragTile;
    if (dragResizeLeft) {
      tile = tile.withX1(tile.x1 + dx);
    } else if (dragResizeRight) {
      tile = tile.withX2(tile.x2 + dx);
    } else {
      int dy = p.y - dragStart.y;
      int currentTop = rowY(tile.row);
      int newCenter = currentTop + dy + rowH(tile.row) / 2;
      int targetRow = rowAtY(newCenter);
      if (!resources.isEmpty()) {
        if (targetRow < 0) {
          targetRow = newCenter < headerHeight() ? 0 : resources.size() - 1;
        }
        targetRow = Math.max(0, Math.min(targetRow, resources.size() - 1));
      } else {
        targetRow = tile.row;
      }
      tile = tile.shift(dx, targetRow - tile.row);
    }
    int minX = TIME_W;
    int maxX = TIME_W + HOURS * Math.max(1, getViewDays()) * colWidth;
    int nx1 = Math.max(minX, Math.min(tile.x1, maxX));
    int nx2 = Math.max(minX, Math.min(tile.x2, maxX));
    dragTile = new Tile(tile.i, tile.row, nx1, nx2, tile.alpha);
    // Update ghost rectangle for visual feedback
    Rectangle r =
        new Rectangle(
            Math.min(nx1, nx2), rowY(dragTile.row), Math.abs(nx2 - nx1), rowH(dragTile.row));
    setGhostDrag(r);
    repaint();
  }

  private void onRelease() {
    altDupPending = false;
    altDupDone = false;
    if (dragTile == null) {
      dragStart = null;
      return;
    }
    Tile finalTile = dragTile;
    dragTile = null;
    dragStart = null;
    clearGhostDrag();
    repaint();
    if (resources.isEmpty()) {
      return;
    }
    int row = Math.max(0, Math.min(finalTile.row, resources.size() - 1));
    Models.Resource resource = resources.get(row);
    Instant start = instantForX(Math.min(finalTile.x1, finalTile.x2));
    Instant end = instantForX(Math.max(finalTile.x1, finalTile.x2));
    Models.Intervention original = finalTile.i;
    boolean changed =
        !resource.id().equals(original.resourceId())
            || !start.equals(original.start())
            || !end.equals(original.end());
    if (!changed) {
      return;
    }
    Models.Intervention before = copyOf(original);
    Models.Intervention updated =
        new Models.Intervention(
            original.id(),
            resource.agencyId(),
            resource.id(),
            original.clientId(),
            original.driverId(),
            original.title(),
            start,
            end,
            original.notes());
    try {
      ensureAvailability(resource.id(), start, end);
      Models.Intervention persisted = dsp.updateIntervention(updated);
      selected = persisted;
      reload();
      notifySuccess("Déplacement appliqué", "Déplacement intervention " + persisted.id());
      pushUpdateHistory("Déplacement", before, persisted);
    } catch (RuntimeException ex) {
      Toolkit.getDefaultToolkit().beep();
      selected = original;
      triggerConflictFlash(original.id());
      JOptionPane.showMessageDialog(
          this, "Erreur de sauvegarde: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
      reload();
    }
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    Optional<Tile> ot = findTileAt(event.getPoint());
    if (ot.isEmpty()) {
      return null;
    }
    Models.Intervention i = ot.get().i;
    Models.Resource resource = resources.stream().filter(r -> r.id().equals(i.resourceId())).findFirst().orElse(null);
    Models.Client client = clients.stream().filter(c -> c.id().equals(i.clientId())).findFirst().orElse(null);
    ZonedDateTime start = i.start().atZone(ZoneId.systemDefault());
    ZonedDateTime end = i.end().atZone(ZoneId.systemDefault());
    StringBuilder sb = new StringBuilder("<html><b>")
        .append(htmlEscape(i.title()))
        .append("</b><br/>")
        .append("Client: ")
        .append(htmlEscape(client != null ? client.name() : i.clientId()))
        .append("<br/>")
        .append("Ressource: ")
        .append(htmlEscape(resource != null ? resource.name() : i.resourceId()))
        .append("<br/>");
    if (i.driverId() != null) {
      sb.append("Chauffeur: ").append(htmlEscape(i.driverId())).append("<br/>");
    }
    Duration duration = Duration.between(i.start(), i.end());
    long totalMinutes = Math.max(0, duration.toMinutes());
    long hours = totalMinutes / 60;
    long minutes = totalMinutes % 60;
    sb.append("Durée: ");
    if (hours > 0) {
      sb.append(hours).append("h");
    }
    if (minutes > 0 || hours == 0) {
      if (hours > 0) {
        sb.append(' ');
      }
      sb.append(minutes).append("min");
    }
    sb.append("<br/>")
        .append(start.toLocalDate())
        .append(' ')
        .append(start.toLocalTime())
        .append(" → ")
        .append(end.toLocalTime());
    if (i.notes() != null && !i.notes().isBlank()) {
      sb.append("<br/>Notes: ")
          .append(htmlEscape(i.notes()).replace("\n", "<br/>"));
    }
    sb.append("</html>");
    return sb.toString();
  }

  private static String htmlEscape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private Models.Intervention copyOf(Models.Intervention it) {
    if (it == null) {
      return null;
    }
    return new Models.Intervention(
        it.id(),
        it.agencyId(),
        it.resourceIds(),
        it.clientId(),
        it.driverId(),
        it.title(),
        it.start(),
        it.end(),
        it.notes(),
        it.internalNotes(),
        it.price());
  }

  private void pushUpdateHistory(String label, Models.Intervention before, Models.Intervention after) {
    if (before == null || after == null) {
      return;
    }
    Models.Intervention beforeCopy = copyOf(before);
    Models.Intervention afterCopy = copyOf(after);
    history.push(
        label,
        () -> {
          try {
            dsp.updateIntervention(beforeCopy);
            selected = beforeCopy;
          } catch (RuntimeException ex) {
            Toolkit.getDefaultToolkit().beep();
          }
          reload();
        },
        () -> {
          try {
            dsp.updateIntervention(afterCopy);
            selected = afterCopy;
          } catch (RuntimeException ex) {
            Toolkit.getDefaultToolkit().beep();
          }
          reload();
        });
  }

  private void maybeOfferQuote(Models.Intervention created) {
    if (created == null) {
      return;
    }
    String clientId = created.clientId();
    if (clientId == null || clientId.isBlank()) {
      return;
    }
    java.awt.Window window = SwingUtilities.getWindowAncestor(this);
    int choice =
        JOptionPane.showConfirmDialog(
            window,
            "Générer un devis pour cette intervention ?",
            "Créer un devis",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
    if (choice != JOptionPane.YES_OPTION) {
      return;
    }
    try {
      Models.Doc doc = dsp.createQuoteFromIntervention(created);
      if (doc != null) {
        notifySuccess("Devis généré", "Génération devis " + doc.id());
        SwingUtilities.invokeLater(() -> new DocumentsFrame(dsp).setVisible(true));
      }
    } catch (UnsupportedOperationException ex) {
      Toolkit.getDefaultToolkit().beep();
      JOptionPane.showMessageDialog(
          window,
          "La source de données ne permet pas de générer automatiquement un devis.",
          "Fonctionnalité indisponible",
          JOptionPane.INFORMATION_MESSAGE);
    } catch (RuntimeException ex) {
      Toolkit.getDefaultToolkit().beep();
      JOptionPane.showMessageDialog(
          window,
          "Erreur lors de la génération du devis : " + ex.getMessage(),
          "Erreur",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void pushCreationHistory(String label, Models.Intervention created) {
    if (created == null) {
      return;
    }
    Models.Intervention template =
        new Models.Intervention(
            null,
            created.agencyId(),
            created.resourceId(),
            created.clientId(),
            created.driverId(),
            created.title(),
            created.start(),
            created.end(),
            created.notes());
    String[] idRef = new String[] {created.id()};
    history.push(
        label,
        () -> {
          String id = idRef[0];
          if (id != null) {
            try {
              dsp.deleteIntervention(id);
            } catch (RuntimeException ex) {
              Toolkit.getDefaultToolkit().beep();
            }
          }
          selected = null;
          reload();
        },
        () -> {
          try {
            Models.Intervention recreated = dsp.createIntervention(template);
            idRef[0] = recreated.id();
            selected = recreated;
          } catch (RuntimeException ex) {
            Toolkit.getDefaultToolkit().beep();
          }
          reload();
        });
  }

  private void pushDeletionHistory(String label, Models.Intervention removed) {
    if (removed == null) {
      return;
    }
    Models.Intervention template =
        new Models.Intervention(
            null,
            removed.agencyId(),
            removed.resourceId(),
            removed.clientId(),
            removed.driverId(),
            removed.title(),
            removed.start(),
            removed.end(),
            removed.notes());
    String[] idRef = new String[] {removed.id()};
    history.push(
        label,
        () -> {
          try {
            Models.Intervention recreated = dsp.createIntervention(template);
            idRef[0] = recreated.id();
            selected = recreated;
          } catch (RuntimeException ex) {
            Toolkit.getDefaultToolkit().beep();
          }
          reload();
        },
        () -> {
          String id = idRef[0];
          if (id != null) {
            try {
              dsp.deleteIntervention(id);
            } catch (RuntimeException ex) {
              Toolkit.getDefaultToolkit().beep();
            }
          }
          selected = null;
          reload();
        });
  }

  private void showHistoryToast(String prefix, String label) {
    if (label == null || label.isBlank()) {
      return;
    }
    java.awt.Window window = SwingUtilities.getWindowAncestor(this);
    if (window != null) {
      Toasts.info(window, prefix + ": " + label);
    }
  }

  private void notifySuccess(String message, String activity) {
    java.awt.Window window = SwingUtilities.getWindowAncestor(this);
    if (window instanceof MainFrame mf) {
      mf.toastSuccess(message);
    } else if (window != null) {
      Toasts.success(window, message);
    }
    ActivityCenter.log(activity);
  }

  private void notifyError(String message) {
    Toolkit.getDefaultToolkit().beep();
    java.awt.Window window = SwingUtilities.getWindowAncestor(this);
    if (window != null) {
      Toasts.error(window, message);
    }
  }

  private void ensureAvailability(String resourceId, Instant start, Instant end) {
    if (resourceId == null || start == null || end == null) {
      return;
    }
    try {
      List<Models.Unavailability> unavailabilityList =
          dsp.listUnavailabilities(
              OffsetDateTime.ofInstant(start, ZoneOffset.UTC),
              OffsetDateTime.ofInstant(end, ZoneOffset.UTC),
              resourceId);
      for (Models.Unavailability unavailability : unavailabilityList) {
        if (!resourceId.equals(unavailability.resourceId())) {
          continue;
        }
        if (unavailability.end().isAfter(start) && unavailability.start().isBefore(end)) {
          String reason = unavailability.reason();
          String detail = (reason == null || reason.isBlank()) ? "" : " : " + reason;
          throw new IllegalStateException("Chevauche une indisponibilité" + detail);
        }
      }
    } catch (RuntimeException ex) {
      String message = ex.getMessage();
      if (message != null && message.toLowerCase(Locale.ROOT).contains("non disponible")) {
        return;
      }
      throw ex;
    }
  }

  private String tileKey(Tile t) {
    if (t == null || t.i == null) {
      return null;
    }
    String id = t.i.id();
    if (id != null) {
      return id;
    }
    String resource = t.i.resourceId() == null ? "" : t.i.resourceId();
    String start = t.i.start() == null ? "" : t.i.start().toString();
    String end = t.i.end() == null ? "" : t.i.end().toString();
    return resource + "|" + start + "|" + end;
  }

  private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxWidth, int maxHeight) {
    if (text == null || text.isBlank()) {
      return;
    }
    FontMetrics fm = g2.getFontMetrics();
    int lineHeight = fm.getHeight();
    int availableLines = Math.max(1, maxHeight / lineHeight);
    List<String> lines = new ArrayList<>();
    String[] words = text.split("\\s+");
    StringBuilder line = new StringBuilder();
    for (String w : words) {
      String candidate = line.length() == 0 ? w : line + " " + w;
      if (stringWidth(fm, candidate) <= maxWidth) {
        line.setLength(0);
        line.append(candidate);
      } else {
        if (line.length() > 0) {
          lines.add(line.toString());
          line.setLength(0);
          line.append(w);
        } else {
          lines.add(w);
          line.setLength(0);
        }
      }
    }
    if (line.length() > 0) {
      lines.add(line.toString());
    }
    boolean ellipsis = lines.size() > availableLines;
    int toDraw = Math.min(availableLines, lines.size());
    int baseline = y + fm.getAscent();
    for (int i = 0; i < toDraw; i++) {
      String s = lines.get(i);
      if (ellipsis && i == toDraw - 1) {
        while (stringWidth(fm, s + "…") > maxWidth && s.length() > 1) {
          s = s.substring(0, s.length() - 1);
        }
        s = s + "…";
      }
      g2.drawString(s, x, baseline + i * lineHeight);
    }
  }

  private int stringWidth(FontMetrics metrics, String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    java.awt.Font font = metrics.getFont();
    String key =
        font.getName()
            + '|'
            + font.getStyle()
            + '|'
            + font.getSize()
            + '|'
            + text;
    Integer cached = textWidthCache.get(key);
    if (cached != null) {
      return cached;
    }
    int width = metrics.stringWidth(text);
    textWidthCache.put(key, width);
    return width;
  }

  private void paintHoverGlow(Graphics2D g2, int x, int y, int w, int h) {
    Stroke old = g2.getStroke();
    for (int r = 0; r < 3; r++) {
      g2.setColor(new Color(255, 255, 255, 60 - r * 15));
      g2.setStroke(new BasicStroke(2 + r * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g2.drawRoundRect(x - r, y - r, w + 2 * r, h + 2 * r, 12, 12);
    }
    g2.setStroke(old);
  }

  private void computeDynamicRows() {
    int resourceCount = resources == null ? 0 : resources.size();
    if (rowHeights == null || rowHeights.length != resourceCount) {
      rowHeights = new int[resourceCount];
    }
    if (rowYPositions == null || rowYPositions.length != resourceCount) {
      rowYPositions = new int[resourceCount];
    }

    int displayCount = displayToResource.size();
    if (displayRowHeights == null || displayRowHeights.length != displayCount) {
      displayRowHeights = new int[displayCount];
      displayRowYPositions = new int[displayCount];
    }

    int y = headerHeight();
    for (int d = 0; d < displayCount; d++) {
      int height;
      Integer mapped = displayToResource.get(d);
      if (mapped == null || mapped < 0) {
        height = headerRowH;
      } else {
        int resourceIndex = mapped;
        String resId = resources.get(resourceIndex).id();
        int lanes = Math.max(1, laneCountByResource.getOrDefault(resId, 1));
        height = Math.max(ROW_H, lanes * 56);
        rowHeights[resourceIndex] = height;
        rowYPositions[resourceIndex] = y;
      }
      displayRowHeights[d] = height;
      displayRowYPositions[d] = y;
      y += height;
    }
  }

  private void ensureRowLayout() {
    int resourceCount = resources == null ? 0 : resources.size();
    int displayCount = displayToResource.size();
    if (rowHeights == null
        || rowHeights.length != resourceCount
        || rowYPositions == null
        || rowYPositions.length != resourceCount
        || displayRowHeights == null
        || displayRowHeights.length != displayCount
        || displayRowYPositions == null
        || displayRowYPositions.length != displayCount) {
      computeDynamicRows();
    }
  }

  private int rowH(int r) {
    ensureRowLayout();
    if (rowHeights == null || r < 0 || r >= rowHeights.length) {
      return ROW_H;
    }
    int h = rowHeights[r];
    return h > 0 ? h : ROW_H;
  }

  private int rowY(int r) {
    ensureRowLayout();
    if (rowYPositions == null || r < 0 || r >= rowYPositions.length) {
      return headerHeight() + r * ROW_H;
    }
    return rowYPositions[r];
  }

  private int displayRowH(int idx) {
    ensureRowLayout();
    if (displayRowHeights == null || idx < 0 || idx >= displayRowHeights.length) {
      return headerRowH;
    }
    int h = displayRowHeights[idx];
    return h > 0 ? h : headerRowH;
  }

  private int displayRowY(int idx) {
    ensureRowLayout();
    if (displayRowYPositions == null || idx < 0 || idx >= displayRowYPositions.length) {
      return headerHeight() + idx * headerRowH;
    }
    return displayRowYPositions[idx];
  }

  private int displayIndexForResource(int resourceIndex) {
    if (resourceDisplayIndex == null
        || resourceIndex < 0
        || resourceIndex >= resourceDisplayIndex.length) {
      return -1;
    }
    return resourceDisplayIndex[resourceIndex];
  }

  private int displayRowAtY(int y) {
    if (y < headerHeight()) {
      return -1;
    }
    ensureRowLayout();
    for (int idx = 0; idx < displayToResource.size(); idx++) {
      int top = displayRowY(idx);
      int bottom = top + displayRowH(idx);
      if (y >= top && y < bottom) {
        return idx;
      }
    }
    return -1;
  }

  private int rowAtY(int y) {
    int displayIndex = displayRowAtY(y);
    if (displayIndex < 0 || displayIndex >= displayToResource.size()) {
      return -1;
    }
    Integer mapped = displayToResource.get(displayIndex);
    if (mapped == null || mapped < 0) {
      return -1;
    }
    return mapped;
  }

  private boolean isResourceVisibleInRect(int resourceIndex, java.awt.Rectangle vis) {
    if (resourceIndex < 0 || vis == null) {
      return false;
    }
    int top = rowY(resourceIndex);
    int bottom = top + rowH(resourceIndex);
    return bottom > vis.y && top < vis.y + vis.height;
  }

  private Models.ResourceType findResourceType(String typeId) {
    if (typeId == null) {
      return null;
    }
    if (resourceTypes == null) {
      return null;
    }
    for (Models.ResourceType type : resourceTypes) {
      if (type != null && Objects.equals(type.id(), typeId)) {
        return type;
      }
    }
    return null;
  }

  private int totalResourcesForType(String typeId) {
    if (resourceTypeIdByResource == null || typeId == null) {
      return 0;
    }
    int count = 0;
    for (java.util.Map.Entry<String, String> entry : resourceTypeIdByResource.entrySet()) {
      String tid = entry.getValue();
      if (tid == null) {
        tid = UNTYPED_TYPE_KEY;
      }
      if (Objects.equals(tid, typeId)) {
        count++;
      }
    }
    return count;
  }

  private int visibleResourcesForType(String typeId) {
    if (resources == null || typeId == null) {
      return 0;
    }
    int count = 0;
    for (Models.Resource resource : resources) {
      if (resource == null || resource.id() == null) {
        continue;
      }
      String tid = resourceTypeIdByResource.get(resource.id());
      if (tid == null) {
        tid = UNTYPED_TYPE_KEY;
      }
      if (Objects.equals(tid, typeId)) {
        count++;
      }
    }
    return count;
  }

  private String headerBaseLabel(String typeId) {
    if (typeId == null) {
      return "";
    }
    if (UNTYPED_TYPE_KEY.equals(typeId)) {
      return "Sans type";
    }
    Models.ResourceType type = findResourceType(typeId);
    if (type != null) {
      String label = type.label();
      if (label == null || label.isBlank()) {
        label = type.code();
      }
      if (label != null && !label.isBlank()) {
        return label;
      }
    }
    return typeId;
  }

  private boolean isHeaderCollapsible(String typeId) {
    return typeId != null && !UNTYPED_TYPE_KEY.equals(typeId);
  }

  private String headerLabel(String typeId) {
    String base = headerBaseLabel(typeId);
    if (!isHeaderCollapsible(typeId)) {
      return base;
    }
    int total = totalResourcesForType(typeId);
    int visible = visibleResourcesForType(typeId);
    boolean collapsed = collapsedTypes.contains(typeId);
    String arrow = collapsed ? "\u25B6" : "\u25BC";
    if (total > 0) {
      return arrow + " " + base + " (" + visible + "/" + total + ")";
    }
    return arrow + " " + base;
  }

  private void registerUndoShortcuts() {
    javax.swing.InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    javax.swing.ActionMap am = getActionMap();
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
    am.put(
        "undo",
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            undoLast();
          }
        });
    im.put(
        KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
        "redo");
    am.put(
        "redo",
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            redoLast();
          }
        });
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.META_DOWN_MASK), "undoMeta");
    am.put(
        "undoMeta",
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            undoLast();
          }
        });
    im.put(
        KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
        "redoMeta");
    am.put(
        "redoMeta",
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            redoLast();
          }
        });
  }

  private void registerConflictNavigationShortcuts() {
    javax.swing.InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    javax.swing.ActionMap actionMap = getActionMap();
    inputMap.put(
        javax.swing.KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_RIGHT,
            java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.ALT_DOWN_MASK),
        "nextConflict");
    actionMap.put(
        "nextConflict",
        new javax.swing.AbstractAction() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            centerOn(pickNextConflict(false));
          }
        });
    inputMap.put(
        javax.swing.KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_LEFT,
            java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.ALT_DOWN_MASK),
        "prevConflict");
    actionMap.put(
        "prevConflict",
        new javax.swing.AbstractAction() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            centerOn(pickNextConflict(true));
          }
        });
  }

  private boolean isInConflict(String interventionId) {
    if (interventionId == null || conflicts == null || conflicts.isEmpty()) {
      return false;
    }
    for (ConflictUtil.Conflict conflict : conflicts) {
      if (conflict == null) {
        continue;
      }
      if (conflict.a() != null && interventionId.equals(conflict.a().id())) {
        return true;
      }
      if (conflict.b() != null && interventionId.equals(conflict.b().id())) {
        return true;
      }
    }
    return false;
  }

  public void selectAndRevealIntervention(String id) {
    if (id == null || interventions == null || interventions.isEmpty()) {
      return;
    }
    for (Models.Intervention intervention : interventions) {
      if (!id.equals(intervention.id())) {
        continue;
      }
      centerOn(intervention);
      SwingUtilities.invokeLater(
          () -> {
            Models.Intervention current =
                interventions.stream()
                    .filter(i -> id.equals(i.id()))
                    .findFirst()
                    .orElse(null);
            if (current == null) {
              return;
            }
            int row = indexOfResource(current.resourceId());
            if (row < 0) {
              return;
            }
            java.awt.Rectangle rect = tileRect(tileFor(current, row));
            if (rect == null) {
              return;
            }
            java.awt.Rectangle padded = new java.awt.Rectangle(rect);
            padded.grow(16, 16);
            scrollRectToVisible(padded);
          });
      return;
    }
  }

  private record Tile(Models.Intervention i, int row, int x1, int x2, float alpha) {
    private Tile(Models.Intervention i, int row, int x1, int x2) {
      this(i, row, x1, x2, 1f);
    }

    private Tile shift(int dx, int rowDelta) {
      return new Tile(i, row + rowDelta, x1 + dx, x2 + dx, alpha);
    }

    private Tile withX1(int nx1) {
      return new Tile(i, row, nx1, x2, alpha);
    }

    private Tile withX2(int nx2) {
      return new Tile(i, row, x1, nx2, alpha);
    }

    private Tile withAlpha(float a) {
      return new Tile(i, row, x1, x2, a);
    }
  }

  public void nextConflict() {
    centerOn(pickNextConflict(false));
  }

  public void prevConflict() {
    centerOn(pickNextConflict(true));
  }

  public java.util.Map<String, Integer> getVisibleRecapByResource() {
    java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
    if (resources == null || interventions == null) {
      return counts;
    }
    java.util.Map<String, String> nameById = new java.util.HashMap<>();
    for (Models.Resource resource : resources) {
      nameById.put(resource.id(), resource.name());
      counts.put(resource.name(), 0);
    }
    for (Models.Intervention intervention : interventions) {
      if (intervention.resourceId() == null) {
        continue;
      }
      String name = nameById.get(intervention.resourceId());
      if (name != null) {
        counts.put(name, counts.getOrDefault(name, 0) + 1);
      }
    }
    return counts;
  }

  public void quickFind(String query) {
    if (query == null || query.isBlank() || interventions == null) {
      return;
    }
    String needle = query.toLowerCase(Locale.ROOT).trim();
    for (Models.Intervention intervention : interventions) {
      if (intervention == null) {
        continue;
      }
      String title = intervention.title();
      if (title != null && title.toLowerCase(Locale.ROOT).contains(needle)) {
        centerOn(intervention);
        return;
      }
    }
  }

  /** Try to resolve a specific conflict by moving the later-starting intervention right after the other's end.
   *  Uses 15-min steps up to 96 tries, checks unavailabilities and recomputed conflicts, persists on success. */
  public void resolveConflict(ConflictUtil.Conflict conflict) {
    if (conflict == null) {
      return;
    }
    Models.Intervention a = conflict.a();
    Models.Intervention b = conflict.b();
    if (a == null || b == null) {
      return;
    }
    if (a.start() == null || b.start() == null || a.end() == null || b.end() == null) {
      Toolkit.getDefaultToolkit().beep();
      JOptionPane.showMessageDialog(
          this,
          "Impossible de résoudre automatiquement ce conflit à proximité.",
          "Conflit",
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    Models.Intervention first = a.start().isBefore(b.start()) ? a : b;
    Models.Intervention later = a.start().isBefore(b.start()) ? b : a;
    if (first.end() == null || later.start() == null || later.end() == null || later.resourceId() == null) {
      Toolkit.getDefaultToolkit().beep();
      JOptionPane.showMessageDialog(
          this,
          "Impossible de résoudre automatiquement ce conflit à proximité.",
          "Conflit",
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    java.time.Duration dur = java.time.Duration.between(later.start(), later.end());
    java.time.Instant targetStart = first.end();
    for (int i = 0; i < 96; i++) {
      java.time.Instant candStart = targetStart.plus(java.time.Duration.ofMinutes(15L * i));
      java.time.Instant candEnd = candStart.plus(dur);
      try {
        ensureAvailability(later.resourceId(), candStart, candEnd);
      } catch (RuntimeException ex) {
        continue;
      }
      java.util.List<Models.Intervention> tmp = new java.util.ArrayList<>(interventions);
      int idx = -1;
      for (int k = 0; k < tmp.size(); k++) {
        if (tmp.get(k).id().equals(later.id())) {
          idx = k;
          break;
        }
      }
      if (idx >= 0) {
        Models.Intervention moved = new Models.Intervention(
            later.id(),
            later.agencyId(),
            later.resourceId(),
            later.clientId(),
            later.driverId(),
            later.title(),
            candStart,
            candEnd,
            later.notes());
        tmp.set(idx, moved);
        var newConf = ConflictUtil.computeConflicts(tmp);
        boolean ok = true;
        for (var c : newConf) {
          if ((c.a() != null && later.id().equals(c.a().id()))
              || (c.b() != null && later.id().equals(c.b().id()))) {
            ok = false;
            break;
          }
        }
        if (ok) {
          try {
            Models.Intervention before = copyOf(later);
            Models.Intervention saved = dsp.updateIntervention(moved);
            selected = saved;
            reload();
            notifySuccess("Conflit résolu", "Intervention " + saved.id() + " décalée");
            pushUpdateHistory("Résolution conflit", before, saved);
            return;
          } catch (RuntimeException ex) {
            // Try next slot
          }
        }
      }
    }
    Toolkit.getDefaultToolkit().beep();
    JOptionPane.showMessageDialog(
        this,
        "Impossible de résoudre automatiquement ce conflit à proximité.",
        "Conflit",
        JOptionPane.WARNING_MESSAGE);
  }

  private void setGhostDrag(Rectangle r) {
    this.ghostDragRect = r;
    repaint();
  }

  private void clearGhostDrag() {
    if (this.ghostDragRect != null) {
      this.ghostDragRect = null;
      repaint();
    }
  }

  public void addQuickTagToSelection(String tag) {
    if (selected == null) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    String id = selected.id();
    if (id == null || id.isBlank()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    String trimmed = tag == null ? "" : tag.trim();
    if (trimmed.isEmpty()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    java.util.List<String> before = interventionTags.getOrDefault(id, java.util.List.of());
    boolean alreadyPresent = before.stream().anyMatch(t -> t != null && t.equalsIgnoreCase(trimmed));
    if (alreadyPresent) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    java.util.List<String> after = new java.util.ArrayList<>(before);
    after.add(trimmed);
    try {
      dsp.setInterventionTags(id, after);
      interventionTags.put(id, java.util.List.copyOf(after));
      notifySuccess("Tag ajouté", "Tag " + trimmed + " ajouté à " + id);
      java.util.List<String> beforeSnapshot = java.util.List.copyOf(before);
      java.util.List<String> afterSnapshot = java.util.List.copyOf(after);
      history.push(
          "Tag " + trimmed,
          () -> {
            try {
              dsp.setInterventionTags(id, beforeSnapshot);
            } catch (RuntimeException ex) {
              Toolkit.getDefaultToolkit().beep();
            }
            reload();
            selectAndRevealIntervention(id);
          },
          () -> {
            try {
              dsp.setInterventionTags(id, afterSnapshot);
            } catch (RuntimeException ex) {
              Toolkit.getDefaultToolkit().beep();
            }
            reload();
            selectAndRevealIntervention(id);
          });
      reload();
      selectAndRevealIntervention(id);
    } catch (UnsupportedOperationException ex) {
      Toolkit.getDefaultToolkit().beep();
      JOptionPane.showMessageDialog(
          this,
          "La source de données ne permet pas de modifier les tags d'intervention.",
          "Fonctionnalité indisponible",
          JOptionPane.INFORMATION_MESSAGE);
    } catch (RuntimeException ex) {
      Toolkit.getDefaultToolkit().beep();
      JOptionPane.showMessageDialog(
          this,
          "Impossible d'ajouter le tag : " + ex.getMessage(),
          "Erreur",
          JOptionPane.ERROR_MESSAGE);
    }
  }
}
