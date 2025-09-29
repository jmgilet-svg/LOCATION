package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.table.DefaultTableModel;

public class ConflictInspectorFrame extends JFrame {
  private final DataSourceProvider dsp;
  private final PlanningPanel planning;
  private final DefaultTableModel model =
      new DefaultTableModel(new Object[] {"Début", "Fin", "Ressource", "Intervention A", "Intervention B"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };
  private final JTable table = new JTable(model);
  private final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());
  private List<ConflictUtil.Conflict> conflicts = List.of();

  public ConflictInspectorFrame(DataSourceProvider dsp, PlanningPanel planning) {
    super("Alerte conflits");
    this.dsp = dsp;
    this.planning = planning;
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(8, 8));

    table.setRowHeight(24);
    add(new JScrollPane(table), BorderLayout.CENTER);

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(new JButton(new AbstractAction("Rafraîchir") {
      @Override
      public void actionPerformed(ActionEvent e) {
        refresh();
      }
    }));
    toolbar.add(new JButton(new AbstractAction("Afficher dans le planning") {
      @Override
      public void actionPerformed(ActionEvent e) {
        goToSelected();
      }
    }));
    add(toolbar, BorderLayout.NORTH);

    setSize(900, 500);
    setLocationRelativeTo(null);
    refresh();
  }

  private void refresh() {
    conflicts = planning.getConflicts();
    model.setRowCount(0);
    List<Models.Resource> resources = dsp.listResources();
    for (ConflictUtil.Conflict conflict : conflicts) {
      String resourceId = conflict.resourceId();
      Models.Resource resource =
          resources.stream()
              .filter(r -> resourceId != null && resourceId.equals(r.id()))
              .findFirst()
              .orElse(null);
      model.addRow(
          new Object[] {
            formatter.format(conflict.a().start()),
            formatter.format(conflict.a().end()),
            resource == null ? resourceId : resource.name(),
            conflict.a().title(),
            conflict.b().title()
          });
    }
  }

  private void goToSelected() {
    int row = table.getSelectedRow();
    if (row < 0 || row >= conflicts.size()) {
      return;
    }
    ConflictUtil.Conflict conflict = conflicts.get(row);
    if (conflict.a() != null && conflict.a().start() != null) {
      planning.setDay(conflict.a().start().atZone(ZoneId.systemDefault()).toLocalDate());
    }
    planning.requestFocusInWindow();
    planning.repaint();
  }
}
