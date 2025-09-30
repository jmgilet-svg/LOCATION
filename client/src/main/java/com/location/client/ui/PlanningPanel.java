package com.location.client.ui;

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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import com.location.client.ui.uikit.Toasts;

public class PlanningPanel extends JPanel {
  // --- Virtualization & caches ---
  private final java.util.Map<String, java.awt.Rectangle> tileRectCache = new java.util.HashMap<>();
  private int lastColWidth = -1;
  private java.time.OffsetDateTime lastViewFrom = null;
  private java.time.OffsetDateTime lastViewTo = null;
  private int lastWidth = -1;
  private int lastHeight = -1;

  private final DataSourceProvider dsp;
  private List<Models.Agency> agencies = List.of();
  private List<Models.Resource> resources = List.of();
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

  private static final int HEADER_H = 28;
  private static final int ROW_H = 60;
  private static final int TIME_W = 80;
  private static final int HOURS = 12;
  private static final int START_HOUR = 7;
  private static final int SLOT_MINUTES = 15;
  private static final DayOfWeek WEEK_START = DayOfWeek.MONDAY;
  private static final Duration DEFAULT_CREATE_DURATION = Duration.ofHours(2);
  private static final Icon CONFLICT_ICON = SvgIconLoader.load("conflict.svg", 16);

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
        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
          Optional<Tile> hit = findTileAt(e.getPoint());
          if (hit.isPresent()) {
            openQuickEdit(hit.get().i);
          } else {
            createAt(e.getPoint());
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

  private void createAt(Point p) {
    if (p.y < HEADER_H || resources.isEmpty() || clients.isEmpty()) {
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
    resources = fetchedResources;
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
    if (resources == null || resources.isEmpty()) {
      return HEADER_H + ROW_H;
    }
    if (rowHeights == null || rowHeights.length != resources.size()) {
      computeDynamicRows();
    }
    int total = HEADER_H;
    if (rowHeights != null && rowHeights.length == resources.size()) {
      for (int r = 0; r < rowHeights.length; r++) {
        int h = rowHeights[r];
        total += h > 0 ? h : ROW_H;
      }
    } else {
      total += resources.size() * ROW_H;
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
    java.time.OffsetDateTime viewFrom = getViewFrom();
    java.time.OffsetDateTime viewTo = getViewTo();
    int viewDays = Math.max(1, getViewDays());
    int totalCols = Math.max(1, HOURS * viewDays);
    int availableWidth = Math.max(0, w - TIME_W);
    colWidth = Math.max(1, availableWidth / totalCols);
    updateLayoutCacheState(w, h, colWidth, viewFrom, viewTo);
    int dayWidth = colWidth * HOURS;

    computeLanes(viewFrom.toInstant(), viewTo.toInstant());

    int firstVisibleRow = 0;
    int lastVisibleRow = resources.size() - 1;
    if (resources.isEmpty()) {
      lastVisibleRow = -1;
    } else {
      int bodyTop = HEADER_H;
      int bodyBottom = bodyTop + resources.size() * ROW_H;
      int viewStartY = Math.max(bodyTop, vis.y);
      int viewEndY = Math.min(bodyBottom, vis.y + vis.height);
      if (viewEndY <= bodyTop || viewStartY >= bodyBottom) {
        firstVisibleRow = 0;
        lastVisibleRow = -1;
      } else {
        firstVisibleRow = Math.max(0, (viewStartY - bodyTop) / ROW_H);
        int lastPixel = Math.max(viewStartY, viewEndY - 1);
        lastVisibleRow =
            Math.min(resources.size() - 1, Math.max(firstVisibleRow, (lastPixel - bodyTop) / ROW_H));
      }
    }

    g2.setColor(new Color(250, 250, 255));
    g2.fillRect(0, 0, TIME_W, h);
    g2.setColor(new Color(245, 245, 245));
    if (hoverRow >= firstVisibleRow && hoverRow <= lastVisibleRow) {
      int hy = HEADER_H + hoverRow * ROW_H;
      g2.setColor(new Color(100, 149, 237, 28));
      g2.fillRect(TIME_W, hy, Math.max(0, w - TIME_W), h);
      g2.setColor(new Color(245, 245, 245));
    }
    if (!resources.isEmpty()) {
      g2.setColor(new Color(230, 230, 230));
      int bodyWidth = Math.max(0, w - TIME_W);
      for (int r = 0; r < resources.size(); r++) {
        int yRow = rowY(r);
        g2.drawLine(TIME_W, yRow, TIME_W + bodyWidth, yRow);
      }
      int bottom = rowY(resources.size() - 1) + rowH(resources.size() - 1);
      g2.drawLine(TIME_W, bottom, TIME_W + bodyWidth, bottom);
      g2.setColor(new Color(245, 245, 245));
    }
    g2.fillRect(TIME_W, 0, Math.max(0, w - TIME_W), HEADER_H);
    g2.setColor(Color.GRAY);
    g2.drawLine(0, HEADER_H, w, HEADER_H);
    g2.drawLine(TIME_W, 0, TIME_W, h);

    paintHeatmap(g2, viewDays, dayWidth, h);

    LocalDate headerStart = getViewStart();
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
      g2.drawString(label, xDay + 6, 16);
      g2.setColor(Color.GRAY);
      for (int i = 0; i <= HOURS; i++) {
        int x = xDay + i * colWidth;
        g2.drawLine(x, HEADER_H, x, h);
        if (d == 0 && i < HOURS) {
          String txt = (START_HOUR + i) + ":00";
          g2.drawString(txt, x + 4, HEADER_H - 8);
        }
      }
    }
    int boundaryX = TIME_W + viewDays * dayWidth;
    g2.drawLine(boundaryX, 0, boundaryX, h);

    g2.setColor(Color.GRAY);
    if (lastVisibleRow >= firstVisibleRow && lastVisibleRow >= 0) {
      for (int r = firstVisibleRow; r <= lastVisibleRow; r++) {
        int y = HEADER_H + r * ROW_H;
        g2.drawLine(0, y, w, y);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(resources.get(r).name(), 8, y + 18);
        g2.setColor(Color.GRAY);
      }
    }
    if (!resources.isEmpty()) {
      int bottom = rowY(resources.size() - 1) + rowH(resources.size() - 1);
      g2.drawLine(0, bottom, w, bottom);
    }

    for (Models.Unavailability unav : unavailabilities) {
      int row = indexOfResource(unav.resourceId());
      if (row < firstVisibleRow || row > lastVisibleRow) {
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
      if (r < firstVisibleRow || r > lastVisibleRow) {
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
      if (r < firstVisibleRow || r > lastVisibleRow) {
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
      if (row >= firstVisibleRow && row <= lastVisibleRow) {
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

    if (dragTile != null
        && dragTile.row >= firstVisibleRow
        && dragTile.row <= lastVisibleRow) {
      paintTile(g2, dragTile.withAlpha(0.6f));
    }
  }

  private void paintHeatmap(Graphics2D g2, int viewDays, int dayWidth, int height) {
    if (interventions.isEmpty() || dayWidth <= 0) {
      return;
    }
    int stepsPerDay = HOURS * 2;
    int bodyHeight = Math.max(0, height - HEADER_H);
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
        g2.fillRect(x1, HEADER_H, Math.max(1, x2 - x1), bodyHeight);
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
    int baseY = HEADER_H + t.row * ROW_H + 6;
    int baseHeight = ROW_H - 12;
    int x = Math.min(t.x1, t.x2);
    int w = Math.max(16, Math.abs(t.x2 - t.x1));
    int innerHeight = Math.max(18, baseHeight / laneCount);
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
    if (p == null || p.y < HEADER_H || p.x < TIME_W) {
      return Optional.empty();
    }
    int row = (p.y - HEADER_H) / ROW_H;
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
    if (p.y < HEADER_H || p.x < TIME_W || newHoverRow < 0 || newHoverRow >= resources.size()) {
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
          targetRow = newCenter < HEADER_H ? 0 : resources.size() - 1;
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
        it.resourceId(),
        it.clientId(),
        it.driverId(),
        it.title(),
        it.start(),
        it.end(),
        it.notes());
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
    int n = resources == null ? 0 : resources.size();
    rowHeights = new int[n];
    rowYPositions = new int[n];
    int y = HEADER_H;
    for (int r = 0; r < n; r++) {
      String resId = resources.get(r).id();
      int lanes = Math.max(1, laneCountByResource.getOrDefault(resId, 1));
      int h = Math.max(ROW_H, lanes * 56);
      rowHeights[r] = h;
      rowYPositions[r] = y;
      y += h;
    }
  }

  private int rowH(int r) {
    if (rowHeights == null || r < 0 || r >= rowHeights.length) {
      return ROW_H;
    }
    int h = rowHeights[r];
    return h > 0 ? h : ROW_H;
  }

  private int rowY(int r) {
    if (rowYPositions == null || r < 0 || r >= rowYPositions.length) {
      return HEADER_H + r * ROW_H;
    }
    return rowYPositions[r];
  }

  private int rowAtY(int y) {
    if (y < HEADER_H) {
      return -1;
    }
    int size = resources == null ? 0 : resources.size();
    if (rowYPositions == null || rowYPositions.length == 0) {
      int row = (y - HEADER_H) / ROW_H;
      return (row >= 0 && row < size) ? row : -1;
    }
    for (int r = 0; r < rowYPositions.length; r++) {
      int top = rowYPositions[r];
      int bottom = top + rowH(r);
      if (y >= top && y < bottom) {
        return r;
      }
    }
    return -1;
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
}
