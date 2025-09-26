package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
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

public class PlanningPanel extends JPanel {
  private final DataSourceProvider dsp;
  private List<Models.Agency> agencies = List.of();
  private List<Models.Resource> resources = List.of();
  private List<Models.Client> clients = List.of();
  private List<Models.Intervention> interventions = List.of();
  private List<Models.Unavailability> unavailabilities = List.of();
  private final List<Runnable> reloadListeners = new ArrayList<>();
  public interface SelectionListener {
    void onSelection(List<Models.Intervention> selection, List<Models.Intervention> dayItems);
  }

  private final List<SelectionListener> selectionListeners = new ArrayList<>();
  private LocalDate day = LocalDate.now();
  private String filterAgencyId;
  private String filterResourceId;
  private String filterClientId;
  private String filterQuery = "";
  private String filterTags = "";

  private static final int HEADER_H = 28;
  private static final int ROW_H = 60;
  private static final int TIME_W = 80;
  private static final int HOURS = 12;
  private static final int START_HOUR = 7;
  private static final int SLOT_MINUTES = 15;
  private static final DayOfWeek WEEK_START = DayOfWeek.MONDAY;
  private static final Duration DEFAULT_CREATE_DURATION = Duration.ofHours(2);

  private int colWidth;
  private Tile dragTile;
  private Point dragStart;
  private boolean dragResizeLeft;
  private boolean dragResizeRight;
  private Models.Intervention selected;
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

    MouseAdapter adapter = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
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
    };
    addMouseListener(adapter);
    addMouseMotionListener(adapter);
  }

  private void createAt(Point p) {
    if (p.y < HEADER_H || resources.isEmpty() || clients.isEmpty()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    if (findTileAt(p).isPresent()) {
      return;
    }
    int row = (p.y - HEADER_H) / ROW_H;
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
      Models.Intervention created = dsp.createIntervention(payload);
      selected = created;
      reload();
      notifySuccess("Intervention créée", "Création intervention " + created.id());
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
            .onSaved(
                saved -> {
                  selected = saved;
                  reload();
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
    if (filterTags != null && !filterTags.isBlank()) {
      Set<String> visibleIds = resources.stream().map(Models.Resource::id).collect(Collectors.toSet());
      data = data.stream().filter(i -> visibleIds.contains(i.resourceId())).toList();
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

  public List<Models.Resource> getResources() {
    return resources;
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
    if (selected == null) {
      Toolkit.getDefaultToolkit().beep();
      return false;
    }
    Models.Intervention base = selected;
    Instant start = base.start().plus(Duration.ofHours(1));
    Instant end = base.end().plus(Duration.ofHours(1));
    if (!end.isAfter(start)) {
      Toolkit.getDefaultToolkit().beep();
      return false;
    }
    Models.Intervention payload =
        new Models.Intervention(
            null,
            base.agencyId(),
            base.resourceId(),
            base.clientId(),
            base.driverId(),
            base.title() + " (copie)",
            start,
            end,
            base.notes());
    try {
      Models.Intervention created = dsp.createIntervention(payload);
      selected = created;
      reload();
      notifySuccess("Intervention dupliquée", "Duplication intervention " + created.id());
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
      Models.Intervention persisted = dsp.updateIntervention(updated);
      selected = persisted;
      reload();
      notifySuccess("Intervention décalée", "Déplacement intervention " + persisted.id());
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

  public void clearSelection() {
    if (selected != null) {
      selected = null;
      repaint();
      fireSelectionChanged();
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int w = getWidth();
    int h = getHeight();
    int viewDays = Math.max(1, getViewDays());
    int totalCols = Math.max(1, HOURS * viewDays);
    int availableWidth = Math.max(0, w - TIME_W);
    colWidth = Math.max(1, availableWidth / totalCols);
    int dayWidth = colWidth * HOURS;

    g2.setColor(new Color(250, 250, 255));
    g2.fillRect(0, 0, TIME_W, h);
    g2.setColor(new Color(245, 245, 245));
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
    for (int r = 0; r < resources.size(); r++) {
      int y = HEADER_H + r * ROW_H;
      g2.drawLine(0, y, w, y);
      g2.setColor(Color.DARK_GRAY);
      g2.drawString(resources.get(r).name(), 8, y + 18);
      g2.setColor(Color.GRAY);
    }

    for (Models.Unavailability unav : unavailabilities) {
      int row = indexOfResource(unav.resourceId());
      if (row < 0) {
        continue;
      }
      int x1 = xForInstant(unav.start());
      int x2 = xForInstant(unav.end());
      int y = HEADER_H + row * ROW_H + 6;
      int height = ROW_H - 12;
      paintHatched(g2, Math.min(x1, x2), y, Math.max(12, Math.abs(x2 - x1)), height, unav.recurring());
    }

    for (Models.Intervention i : interventions) {
      int r = indexOfResource(i.resourceId());
      if (r < 0) continue;
      Tile t = tileFor(i, r);
      paintTile(g2, t);
    }

    g2.setFont(getFont());
    for (Models.Intervention i : interventions) {
      if (i.notes() == null || i.notes().isBlank()) {
        continue;
      }
      int r = indexOfResource(i.resourceId());
      if (r < 0) {
        continue;
      }
      Tile t = tileFor(i, r);
      int iconX = Math.max(t.x1, t.x2) - 18;
      int iconY = HEADER_H + r * ROW_H + 18;
      g2.setColor(new Color(30, 30, 30, 200));
      g2.drawString("\uD83D\uDCD3", iconX, iconY);
    }

    if (selected != null) {
      int row = indexOfResource(selected.resourceId());
      if (row >= 0) {
        Tile t = tileFor(selected, row);
        int x = Math.min(t.x1, t.x2);
        int w = Math.max(16, Math.abs(t.x2 - t.x1));
        int y = HEADER_H + row * ROW_H + 4;
        int height = ROW_H - 8;
        Stroke old = g2.getStroke();
        g2.setColor(new Color(255, 200, 0, 180));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(x - 4, y, w + 8, height, 14, 14);
        g2.setStroke(old);
      }
    }

    if (dragTile != null) {
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
    int y = HEADER_H + t.row * ROW_H + 6;
    int height = ROW_H - 12;
    int x = Math.min(t.x1, t.x2);
    int w = Math.max(16, Math.abs(t.x2 - t.x1));

    boolean conflict = hasConflict(t);
    Color base = new Color(66, 133, 244);
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
    g2.setComposite(AlphaComposite.SrcOver);

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
    g2.drawString(t.i.title(), x + 8, y + 18);
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
    Instant s = instantForX(Math.min(t.x1, t.x2));
    Instant e = instantForX(Math.max(t.x1, t.x2));
    String rId = resources.get(Math.max(0, Math.min(resources.size() - 1, t.row))).id();
    for (Models.Intervention i : interventions) {
      if (!i.resourceId().equals(rId) || i == t.i) continue;
      if (i.end().isAfter(s) && i.start().isBefore(e)) return true;
    }
    return false;
  }

  private Optional<Tile> findTileAt(Point p) {
    for (Models.Intervention i : interventions) {
      int r = indexOfResource(i.resourceId());
      Tile t = tileFor(i, r);
      int y = HEADER_H + r * ROW_H + 6;
      int height = ROW_H - 12;
      int x = Math.min(t.x1, t.x2);
      int w = Math.max(16, Math.abs(t.x2 - t.x1));
      Rectangle rect = new Rectangle(x, y, w, height);
      if (rect.contains(p)) return Optional.of(t);
    }
    return Optional.empty();
  }

  public boolean deleteSelected() {
    if (selected == null) {
      return false;
    }
    try {
      dsp.deleteIntervention(selected.id());
      selected = null;
      reload();
      return true;
    } catch (RuntimeException ex) {
      JOptionPane.showMessageDialog(
          this, "Suppression impossible: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }

  private void nudgeTime(int minutes) {
    if (selected == null) {
      return;
    }
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
      Models.Intervention persisted = dsp.updateIntervention(updated);
      selected = persisted;
      reload();
    } catch (RuntimeException ex) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  private void nudgeResource(int delta) {
    if (selected == null || resources.isEmpty()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
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
      Models.Intervention persisted = dsp.updateIntervention(updated);
      selected = persisted;
      reload();
    } catch (RuntimeException ex) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  private void updateCursor(Point p) {
    Optional<Tile> ot = findTileAt(p);
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
      int rowDelta = Math.round(dy / (float) ROW_H);
      int targetRow = resources.isEmpty() ? 0 : Math.max(0, Math.min(tile.row + rowDelta, resources.size() - 1));
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
      Models.Intervention persisted = dsp.updateIntervention(updated);
      selected = persisted;
      reload();
      notifySuccess("Déplacement appliqué", "Déplacement intervention " + persisted.id());
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

  private void notifySuccess(String message, String activity) {
    java.awt.Window window = SwingUtilities.getWindowAncestor(this);
    if (window instanceof MainFrame mf) {
      mf.toastSuccess(message);
    } else if (window != null) {
      Toast.success(window, message);
    }
    ActivityCenter.log(activity);
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
}
