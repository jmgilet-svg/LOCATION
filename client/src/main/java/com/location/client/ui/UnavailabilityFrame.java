package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.table.DefaultTableModel;

public class UnavailabilityFrame extends JFrame {
  private final DataSourceProvider dataSourceProvider;
  private final JComboBox<Models.Resource> resourceCombo = new JComboBox<>();
  private final DefaultTableModel model;
  private final JTable table;

  public UnavailabilityFrame(DataSourceProvider dsp) {
    super("Indisponibilités ressources");
    this.dataSourceProvider = dsp;
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(6, 6));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Ressource:"));
    for (Models.Resource resource : dataSourceProvider.listResources()) {
      resourceCombo.addItem(resource);
    }
    resourceCombo.addActionListener(e -> refresh());
    top.add(resourceCombo);

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(
        new JButton(
            new AbstractAction("Ajouter") {
              @Override
              public void actionPerformed(ActionEvent e) {
                addRow();
              }
            }));
    toolbar.add(
        new JButton(
            new AbstractAction("Supprimer") {
              @Override
              public void actionPerformed(ActionEvent e) {
                deleteSelected();
              }
            }));
    toolbar.add(
        new JButton(
            new AbstractAction("Enregistrer") {
              @Override
              public void actionPerformed(ActionEvent e) {
                saveAll();
              }
            }));
    top.add(toolbar);
    add(top, BorderLayout.NORTH);

    model =
        new DefaultTableModel(
            new Object[] {"ID", "Jour", "Début", "Fin", "Récurrence", "Date début", "Date fin", "Raison"},
            0) {
          @Override
          public boolean isCellEditable(int row, int column) {
            return column != 0;
          }
        };
    table = new JTable(model);
    table.setPreferredScrollableViewportSize(new Dimension(880, 360));
    table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(dayEditor()));
    table.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(recurrenceEditor()));
    add(new JScrollPane(table), BorderLayout.CENTER);

    setSize(960, 520);
    setLocationRelativeTo(null);

    if (resourceCombo.getItemCount() > 0) {
      resourceCombo.setSelectedIndex(0);
    }
  }

  private JComboBox<String> dayEditor() {
    JComboBox<String> combo = new JComboBox<>();
    for (DayOfWeek day : DayOfWeek.values()) {
      combo.addItem(day.name());
    }
    return combo;
  }

  private JComboBox<String> recurrenceEditor() {
    JComboBox<String> combo = new JComboBox<>();
    combo.addItem("Aucune");
    combo.addItem("Hebdo");
    return combo;
  }

  private void refresh() {
    model.setRowCount(0);
    Models.Resource resource = (Models.Resource) resourceCombo.getSelectedItem();
    if (resource == null) {
      return;
    }
    try {
      for (Models.Unavailability unavailability : dataSourceProvider.listUnavailability(resource.id())) {
        LocalDateTime start = LocalDateTime.ofInstant(unavailability.start(), ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(unavailability.end(), ZoneId.systemDefault());
        model.addRow(
            new Object[] {
              unavailability.id(),
              start.getDayOfWeek().name(),
              start.toLocalTime().toString(),
              end.toLocalTime().toString(),
              "Aucune",
              start.toLocalDate().toString(),
              end.toLocalDate().toString(),
              unavailability.reason()
            });
      }
    } catch (RuntimeException ex) {
      Toast.error(this, ex.getMessage());
      return;
    }
    try {
      for (Models.RecurringUnavailability recurring :
          dataSourceProvider.listRecurringUnavailability(resource.id())) {
        model.addRow(
            new Object[] {
              recurring.id(),
              recurring.dayOfWeek().name(),
              recurring.start().toString(),
              recurring.end().toString(),
              "Hebdo",
              "",
              "",
              recurring.reason()
            });
      }
    } catch (RuntimeException ex) {
      Toast.error(this, ex.getMessage());
    }
  }

  private void addRow() {
    model.addRow(
        new Object[] {
          null,
          DayOfWeek.MONDAY.name(),
          LocalTime.of(8, 0).toString(),
          LocalTime.of(10, 0).toString(),
          "Hebdo",
          LocalDate.now().toString(),
          LocalDate.now().toString(),
          "Maintenance"
        });
  }

  private void deleteSelected() {
    int row = table.getSelectedRow();
    if (row < 0) {
      return;
    }
    String id = stringValue(model.getValueAt(row, 0));
    String recurrence = stringValue(model.getValueAt(row, 4)).toLowerCase(Locale.ROOT);
    try {
      if (!id.isBlank()) {
        if ("hebdo".equals(recurrence)) {
          dataSourceProvider.deleteRecurringUnavailability(id);
        } else {
          dataSourceProvider.deleteUnavailability(id);
        }
      }
      model.removeRow(row);
      Toast.success(this, "Indisponibilité supprimée");
    } catch (RuntimeException ex) {
      Toast.error(this, ex.getMessage());
    }
  }

  private void saveAll() {
    Models.Resource resource = (Models.Resource) resourceCombo.getSelectedItem();
    if (resource == null) {
      return;
    }
    boolean updated = false;
    ZoneId zone = ZoneId.systemDefault();
    for (int row = 0; row < model.getRowCount(); row++) {
      String recurrence = stringValue(model.getValueAt(row, 4)).toLowerCase(Locale.ROOT);
      String reason = stringValue(model.getValueAt(row, 7));
      try {
        if ("hebdo".equals(recurrence)) {
          String idRaw = stringValue(model.getValueAt(row, 0));
          String dayRaw = stringValue(model.getValueAt(row, 1));
          DayOfWeek day = DayOfWeek.valueOf(dayRaw.toUpperCase(Locale.ROOT));
          LocalTime start = LocalTime.parse(stringValue(model.getValueAt(row, 2)));
          LocalTime end = LocalTime.parse(stringValue(model.getValueAt(row, 3)));
          Models.RecurringUnavailability saved =
              dataSourceProvider.saveRecurringUnavailability(
                  new Models.RecurringUnavailability(
                      idRaw.isBlank() ? null : idRaw,
                      resource.id(),
                      day,
                      start,
                      end,
                      reason));
          model.setValueAt(saved.id(), row, 0);
        } else {
          String idRaw = stringValue(model.getValueAt(row, 0));
          LocalDate fromDate = LocalDate.parse(stringValue(model.getValueAt(row, 5)));
          String toDateRaw = stringValue(model.getValueAt(row, 6));
          LocalDate toDate = toDateRaw.isBlank() ? fromDate : LocalDate.parse(toDateRaw);
          LocalTime start = LocalTime.parse(stringValue(model.getValueAt(row, 2)));
          LocalTime end = LocalTime.parse(stringValue(model.getValueAt(row, 3)));
          Instant startInstant = fromDate.atTime(start).atZone(zone).toInstant();
          Instant endInstant = toDate.atTime(end).atZone(zone).toInstant();
          Models.Unavailability saved =
              dataSourceProvider.saveUnavailability(
                  new Models.Unavailability(
                      idRaw.isBlank() ? null : idRaw,
                      resource.id(),
                      reason,
                      startInstant,
                      endInstant,
                      false));
          model.setValueAt(saved.id(), row, 0);
        }
        updated = true;
      } catch (RuntimeException ex) {
        Toast.error(this, ex.getMessage());
        table.setRowSelectionInterval(row, row);
        return;
      }
    }
    if (updated) {
      Toast.success(this, "Indisponibilités enregistrées");
      refresh();
    }
  }

  private String stringValue(Object value) {
    return value == null ? "" : value.toString().trim();
  }
}
