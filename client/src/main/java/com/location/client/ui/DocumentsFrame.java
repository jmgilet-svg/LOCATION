package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DocumentsFrame extends JFrame {
  private static final DocTypeOption[] TYPES =
      new DocTypeOption[] {
        new DocTypeOption(null, "Tous les documents"),
        new DocTypeOption("QUOTE", "Devis"),
        new DocTypeOption("ORDER", "Bons de commande"),
        new DocTypeOption("DELIVERY", "Bons de livraison"),
        new DocTypeOption("INVOICE", "Factures")
      };
  private final DataSourceProvider dsp;
  private final JTable table = new JTable();
  private final DefaultTableModel model =
      new DefaultTableModel(new Object[] {"ID", "Type", "Titre"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };
  private final JComboBox<DocTypeOption> cbType = new JComboBox<>(TYPES);

  public DocumentsFrame(DataSourceProvider dsp) {
    this(dsp, null);
  }

  public DocumentsFrame(DataSourceProvider dsp, String initialType) {
    super("Documents");
    this.dsp = dsp;
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout());

    table.setModel(model);
    add(new JScrollPane(table), BorderLayout.CENTER);

    JToolBar tb = new JToolBar();
    tb.setFloatable(false);
    tb.add(new JLabel("Type: "));
    cbType.setMaximumSize(new Dimension(220, cbType.getPreferredSize().height));
    if (initialType != null && !initialType.isBlank()) {
      selectType(initialType);
    }
    cbType.addActionListener(e -> refresh());
    tb.add(cbType);
    tb.addSeparator();
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
    DocTypeOption selected = (DocTypeOption) cbType.getSelectedItem();
    String type = selected == null ? null : selected.code();
    for (Models.Doc d : dsp.listDocs(type, null)) {
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

  private void selectType(String code) {
    for (int i = 0; i < cbType.getItemCount(); i++) {
      DocTypeOption option = cbType.getItemAt(i);
      if (option != null && java.util.Objects.equals(option.code(), code)) {
        cbType.setSelectedIndex(i);
        return;
      }
    }
  }

  private record DocTypeOption(String code, String label) {
    @Override
    public String toString() {
      return label;
    }
  }
}
