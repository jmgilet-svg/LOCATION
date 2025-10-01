package com.location.client.ui;


import com.location.client.core.Models;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemListener;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import com.location.client.ui.uikit.Notify;


public class ConflictsPanel extends JPanel {
  private final DefaultListModel<ConflictRow> model = new DefaultListModel<>();
  private final JList<ConflictRow> list = new JList<>(model);
  private final JCheckBox onlyVisible = new JCheckBox("Ressources visibles");
  private final JCheckBox onlySelectedDay = new JCheckBox("Jour sélectionné");
  private final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

  public ConflictsPanel() {
    super(new BorderLayout(8, 8));
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(
        new DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(
              JList<?> list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus) {
            Component c =
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ConflictRow row) {
              setText(row.label);
            }
            return c;
          }
        });
    add(new JScrollPane(list), BorderLayout.CENTER);

    JPanel north = new JPanel();
    north.add(onlyVisible);
    north.add(onlySelectedDay);
    JButton btnResolve = new JButton("Résoudre automatiquement");
    north.add(btnResolve);
    btnResolve.addActionListener(e -> {
      int idx = list.getSelectedIndex();
      if (idx >= 0) {
        ConflictRow row = model.get(idx);
        if (row != null && row.conflict != null) {
          Notify.post("conflicts.resolve", row.conflict);
        }
      }
    });
    add(north, BorderLayout.NORTH);

    JPopupMenu menu = new JPopupMenu();
    JMenuItem miResolve = new JMenuItem("Résoudre ce conflit");
    miResolve.addActionListener(e -> {
      int idx = list.getSelectedIndex();
      if (idx >= 0) {
        ConflictRow row = model.get(idx);
        if (row != null && row.conflict != null) {
          Notify.post("conflicts.resolve", row.conflict);
        }
      }
    });
    menu.add(miResolve);
    list.setComponentPopupMenu(menu);
  }

  public void setConflicts(List<ConflictUtil.Conflict> conflicts) {
    SwingUtilities.invokeLater(
        () -> {
          model.clear();
          if (conflicts == null || conflicts.isEmpty()) {
            return;
          }
          for (ConflictUtil.Conflict conflict : conflicts) {
            if (conflict == null) {
              continue;
            }
            String start =
                conflict.a() != null && conflict.a().start() != null
                    ? formatter.format(conflict.a().start())
                    : "?";
            String end =
                conflict.a() != null && conflict.a().end() != null
                    ? formatter.format(conflict.a().end())
                    : "?";
            String aTitle = conflict.a() != null ? safeTitle(conflict.a().title()) : "?";
            String bTitle = conflict.b() != null ? safeTitle(conflict.b().title()) : "?";
            String resource = conflict.resourceId() == null ? "?" : conflict.resourceId();
            model.addElement(
                new ConflictRow(
                    conflict,
                    String.format("[%s - %s] %s — %s (%s)", start, end, aTitle, bTitle, resource)));
          }
        });
  }

  public boolean isOnlyVisible() {
    return onlyVisible.isSelected();
  }

  public void setOnlyVisible(boolean value) {
    onlyVisible.setSelected(value);
  }

  public void addOnlyVisibleListener(ItemListener listener) {
    onlyVisible.addItemListener(listener);
  }

  public boolean isOnlySelectedDay() {
    return onlySelectedDay.isSelected();
  }

  public void setOnlySelectedDay(boolean value) {
    onlySelectedDay.setSelected(value);
  }

  public void addOnlySelectedDayListener(ItemListener listener) {
    onlySelectedDay.addItemListener(listener);
  }

  private void focusConflict(ConflictRow row) {
    if (row == null || row.conflict == null) {
      return;
    }
    Notify.post("conflicts.focus", row.conflict);
  }

  private static String safeTitle(String title) {
    return title == null || title.isBlank() ? "(Sans titre)" : title;
  }

  private record ConflictRow(ConflictUtil.Conflict conflict, String label) {
    @Override
    public String toString() {
      return label;
    }
  }
}
