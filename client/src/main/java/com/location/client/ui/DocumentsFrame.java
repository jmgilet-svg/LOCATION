package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DocumentsFrame extends JFrame {
  private final DataSourceProvider dsp;
  private final JTable table = new JTable();
  private final DefaultTableModel model =
      new DefaultTableModel(new Object[] {"ID", "Type", "Titre"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };

  public DocumentsFrame(DataSourceProvider dsp) {
    super("Documents");
    this.dsp = dsp;
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout());

    table.setModel(model);
    add(new JScrollPane(table), BorderLayout.CENTER);

    JToolBar tb = new JToolBar();
    tb.setFloatable(false);
    tb.add(new JButton(new AbstractAction("Rafraîchir") {
      @Override
      public void actionPerformed(ActionEvent e) {
        refresh();
      }
    }));
    tb.add(new JButton(new AbstractAction("Email…") {
      @Override
      public void actionPerformed(ActionEvent e) {
        openEmail();
      }
    }));
    add(tb, BorderLayout.NORTH);

    setSize(800, 500);
    setLocationRelativeTo(null);
    refresh();
  }

  private void refresh() {
    model.setRowCount(0);
    for (Models.Doc d : dsp.listDocs("ANY", null)) {
      model.addRow(new Object[] {d.id(), d.type(), d.title()});
    }
  }

  private List<String> selectedDocIds() {
    int[] rows = table.getSelectedRows();
    List<String> ids = new ArrayList<>();
    for (int r : rows) {
      ids.add((String) model.getValueAt(r, 0));
    }
    return ids;
  }

  private void openEmail() {
    List<String> ids = selectedDocIds();
    new EmailComposeDialog(this, dsp, ids).setVisible(true);
  }
}
