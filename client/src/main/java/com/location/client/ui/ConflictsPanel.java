package com.location.client.ui;

import com.location.client.core.ConflictUtil;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.Component;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import com.location.client.ui.uikit.Notify;

public class ConflictsPanel extends JPanel {
  private final DefaultListModel<ConflictRow> model = new DefaultListModel<>();
  private final JList<ConflictRow> list = new JList<>(model);
  private final JCheckBox onlyVisible = new JCheckBox("Ressources visibles", true);
  private final JCheckBox onlySelectedDay = new JCheckBox("Jour courant", false);

  public ConflictsPanel() {
    super(new BorderLayout(6, 6));
    list.setCellRenderer(new Renderer());
    add(new JScrollPane(list), BorderLayout.CENTER);
    JPanel north = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
    north.add(new JLabel("Conflits"));
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
    model.clear();
    if (conflicts == null) {
      return;
    }
    for (ConflictUtil.Conflict c : conflicts) {
      model.addElement(new ConflictRow(c));
    }
  }

  private static class ConflictRow {
    final ConflictUtil.Conflict conflict;

    ConflictRow(ConflictUtil.Conflict c) {
      this.conflict = c;
    }
  }

  private static class Renderer extends JLabel implements ListCellRenderer<ConflictRow> {
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Override
    public Component getListCellRendererComponent(
        JList<? extends ConflictRow> list,
        ConflictRow value,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {
      setOpaque(true);
      if (value == null) {
        setText("");
        return this;
      }
      Models.Intervention a = value.conflict.a();
      Models.Intervention b = value.conflict.b();
      String who =
          (a.clientName() != null ? a.clientName() : "?")
              + " ↔ "
              + (b.clientName() != null ? b.clientName() : "?");
      String when = "";
      if (a.start() != null && a.end() != null) {
        when = fmt.format(a.start()) + "–" + fmt.format(a.end());
      }
      setText("<html><b>" + who + "</b><br/>" + when + "</html>");
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }
      return this;
    }
  }
}
