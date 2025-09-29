package com.location.client.ui;

import com.location.client.core.Models;
import com.location.client.core.Preferences;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TopBar extends JPanel {
  private final PlanningPanel planning;
  private final Preferences preferences;
  private final JComboBox<Models.Agency> cbAgency = new JComboBox<>();
  private final JComboBox<Models.Resource> cbResource = new JComboBox<>();
  private final JComboBox<Models.Client> cbClient = new JComboBox<>();
  private final JTextField tfQuery = new JTextField();
  private final JTextField tfTags = new JTextField();
  private final JCheckBox cbNoConflicts = new JCheckBox("Sans conflit");
  private final JSpinner spDate = new JSpinner(new SpinnerDateModel());
  private boolean updating = false;

  public TopBar(PlanningPanel planning, Preferences preferences) {
    this.planning = planning;
    this.preferences = preferences;

    setLayout(new BorderLayout(8, 8));
    JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));

    JButton prev = new JButton("\u25C0");
    JButton next = new JButton("\u25B6");
    JButton today = new JButton("Aujourd'hui");
    prev.addActionListener(e -> prevDay());
    next.addActionListener(e -> nextDay());
    today.addActionListener(e -> setDay(LocalDate.now()));

    spDate.setEditor(new JSpinner.DateEditor(spDate, "yyyy-MM-dd"));
    spDate.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if (updating) {
          return;
        }
        Object value = spDate.getValue();
        if (value instanceof Date date) {
          LocalDate local = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
          setDay(local);
        }
      }
    });

    left.add(prev);
    left.add(next);
    left.add(today);
    left.add(spDate);

    configureRenderer(cbAgency, Models.Agency::name);
    configureRenderer(cbResource, Models.Resource::name);
    configureRenderer(cbClient, Models.Client::name);

    cbAgency.addActionListener(
        e -> {
          if (updating) {
            return;
          }
          Models.Agency agency = (Models.Agency) cbAgency.getSelectedItem();
          String id = agency == null ? null : agency.id();
          planning.setFilterAgency(id);
          preferences.setFilterAgencyId(id);
          preferences.save();
          refreshCombos();
        });
    cbResource.addActionListener(
        e -> {
          if (updating) {
            return;
          }
          Models.Resource resource = (Models.Resource) cbResource.getSelectedItem();
          String id = resource == null ? null : resource.id();
          planning.setFilterResource(id);
          preferences.setFilterResourceId(id);
          preferences.save();
        });
    cbClient.addActionListener(
        e -> {
          if (updating) {
            return;
          }
          Models.Client client = (Models.Client) cbClient.getSelectedItem();
          String id = client == null ? null : client.id();
          planning.setFilterClient(id);
          preferences.setFilterClientId(id);
          preferences.save();
        });

    tfQuery.setColumns(16);
    tfQuery.addActionListener(
        e -> {
          if (updating) {
            return;
          }
          planning.setFilterQuery(tfQuery.getText());
          preferences.setFilterQuery(tfQuery.getText());
          preferences.save();
        });

    tfTags.setColumns(12);
    tfTags.addActionListener(
        e -> {
          if (updating) {
            return;
          }
          planning.setFilterTags(tfTags.getText());
          preferences.setFilterTags(tfTags.getText());
          preferences.save();
        });

    cbNoConflicts.setFocusable(false);
    cbNoConflicts.addActionListener(
        e -> {
          if (updating) {
            return;
          }
          planning.setFilterNoConflicts(cbNoConflicts.isSelected());
        });

    right.add(new JLabel("Agence:"));
    right.add(cbAgency);
    right.add(new JLabel("Ressource:"));
    right.add(cbResource);
    right.add(new JLabel("Client:"));
    right.add(cbClient);
    right.add(new JLabel("Recherche:"));
    right.add(tfQuery);
    right.add(new JLabel("Tags:"));
    right.add(tfTags);
    right.add(cbNoConflicts);

    add(left, BorderLayout.WEST);
    add(right, BorderLayout.EAST);

    String savedDay = preferences.getDayIso();
    if (savedDay != null && !savedDay.isBlank()) {
      setDay(LocalDate.parse(savedDay));
    } else {
      setDay(planning.getDay());
    }
    String savedQuery = preferences.getFilterQuery();
    tfQuery.setText(savedQuery);
    planning.setFilterQuery(savedQuery);
    String savedTags = preferences.getFilterTags();
    tfTags.setText(savedTags);
    planning.setFilterTags(savedTags);
    updating = true;
    try {
      cbNoConflicts.setSelected(planning.isFilterNoConflicts());
    } finally {
      updating = false;
    }
    refreshCombos();
    selectById(cbAgency, Models.Agency::id, preferences.getFilterAgencyId());
    selectById(cbResource, Models.Resource::id, preferences.getFilterResourceId());
    selectById(cbClient, Models.Client::id, preferences.getFilterClientId());
    updating = false;
  }

  public void refreshCombos() {
    updating = true;
    try {
      String agencyId = planning.getFilterAgencyId();
      String resourceId = planning.getFilterResourceId();
      String clientId = planning.getFilterClientId();

      List<Models.Agency> agencies = planning.getAgencies();
      cbAgency.removeAllItems();
      cbAgency.addItem(new Models.Agency(null, "(toutes agences)", null, null, null));
      agencies.forEach(cbAgency::addItem);
      selectById(cbAgency, Models.Agency::id, agencyId);

      List<Models.Resource> resources = planning.getResources();
      cbResource.removeAllItems();
      cbResource.addItem(new Models.Resource(null, "(toutes ressources)", "", null, null, null, null));
      resources.forEach(cbResource::addItem);
      selectById(cbResource, Models.Resource::id, resourceId);

      List<Models.Client> clients = planning.getClients();
      cbClient.removeAllItems();
      cbClient.addItem(
          new Models.Client(null, "(tous clients)", null, null, null, null, null, null, null));
      clients.forEach(cbClient::addItem);
      selectById(cbClient, Models.Client::id, clientId);
    } finally {
      updating = false;
    }
  }

  private static <T> void configureRenderer(JComboBox<T> combo, Function<T, String> labelProvider) {
    combo.setRenderer(
        new DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(
              JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null) {
              @SuppressWarnings("unchecked")
              T typed = (T) value;
              setText(labelProvider.apply(typed));
            } else {
              setText("");
            }
            return this;
          }
        });
  }

  private static <T> void selectById(
      JComboBox<T> comboBox, Function<T, String> idExtractor, String id) {
    if (comboBox.getItemCount() == 0) {
      return;
    }
    if (id == null || id.isBlank()) {
      comboBox.setSelectedIndex(0);
      return;
    }
    for (int i = 0; i < comboBox.getItemCount(); i++) {
      T item = comboBox.getItemAt(i);
      if (item != null && id.equals(idExtractor.apply(item))) {
        comboBox.setSelectedIndex(i);
        return;
      }
    }
    comboBox.setSelectedIndex(0);
  }

  private void setDay(LocalDate day) {
    if (day == null) {
      return;
    }
    planning.setDay(day);
    preferences.setDayIso(day.toString());
    preferences.save();
    updating = true;
    try {
      spDate.setValue(Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    } finally {
      updating = false;
    }
  }

  public void jumpTo(LocalDate day) {
    setDay(day);
  }

  public void prevDay() {
    long step = planning.isWeekMode() ? 7L : 1L;
    setDay(planning.getDay().minusDays(step));
  }

  public void nextDay() {
    long step = planning.isWeekMode() ? 7L : 1L;
    setDay(planning.getDay().plusDays(step));
  }

  public java.time.OffsetDateTime getFrom() {
    return planning.getViewFrom();
  }

  public java.time.OffsetDateTime getTo() {
    return planning.getViewTo();
  }

  public String getResourceId() {
    return planning.getFilterResourceId();
  }

  public String getClientId() {
    return planning.getFilterClientId();
  }

  public String getQuery() {
    return planning.getFilterQuery();
  }

  public String getTags() {
    return planning.getFilterTags();
  }
}
