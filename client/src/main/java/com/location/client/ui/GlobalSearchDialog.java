package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

public class GlobalSearchDialog extends JDialog {
  public record Row(String type, String id, String label) {}

  public interface ResultListener {
    void onOpen(Row row);
  }

  private final DataSourceProvider dataSourceProvider;
  private final JTextField input = new JTextField();
  private final JTable table = new JTable();
  private final List<Row> all = new ArrayList<>();
  private final List<Row> filtered = new ArrayList<>();
  private final Model model = new Model();
  private ResultListener listener;

  public GlobalSearchDialog(Window owner, DataSourceProvider dsp) {
    super(owner, "Recherche globale", ModalityType.APPLICATION_MODAL);
    this.dataSourceProvider = dsp;
    setLayout(new BorderLayout(6, 6));
    add(input, BorderLayout.NORTH);

    table.setModel(model);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
              openSelection();
            }
          }
        });

    add(new JScrollPane(table), BorderLayout.CENTER);

    JPanel footer = new JPanel();
    JButton open = new JButton(new AbstractAction("Ouvrir") {
      @Override
      public void actionPerformed(ActionEvent e) {
        openSelection();
      }
    });
    footer.add(open);
    add(footer, BorderLayout.SOUTH);

    input.addActionListener(e -> openSelection());
    input.getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                filter();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                filter();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                filter();
              }
            });

    setPreferredSize(new Dimension(720, 420));
    pack();
    setLocationRelativeTo(owner);

    loadData();
    filter();
  }

  public GlobalSearchDialog onOpen(ResultListener listener) {
    this.listener = listener;
    return this;
  }

  private void loadData() {
    all.clear();
    try {
      for (Models.Client client : dataSourceProvider.listClients()) {
        all.add(new Row("Client", client.id(), client.name()));
      }
      for (Models.Resource resource : dataSourceProvider.listResources()) {
        StringBuilder label = new StringBuilder(resource.name());
        if (resource.tags() != null && !resource.tags().isBlank()) {
          label.append(" [").append(resource.tags()).append(']');
        }
        if (resource.capacityTons() != null) {
          label.append(" (").append(resource.capacityTons()).append("t)");
        }
        if (resource.licensePlate() != null && !resource.licensePlate().isBlank()) {
          label.append(" – ").append(resource.licensePlate());
        }
        all.add(new Row("Ressource", resource.id(), label.toString()));
      }
      for (Models.Doc doc : dataSourceProvider.listDocs(null, null)) {
        all.add(new Row("Document", doc.id(), doc.title()));
      }
    } catch (RuntimeException ex) {
      JOptionPane.showMessageDialog(
          this,
          "Erreur lors du chargement des données : " + ex.getMessage(),
          "Recherche globale",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void filter() {
    String query = input.getText() == null ? "" : input.getText().trim().toLowerCase();
    filtered.clear();
    if (query.isBlank()) {
      model.fireTableDataChanged();
      return;
    }
    for (Row row : all) {
      if (matches(row, query)) {
        filtered.add(row);
      }
    }
    model.fireTableDataChanged();
    if (!filtered.isEmpty()) {
      table.setRowSelectionInterval(0, 0);
    }
  }

  private boolean matches(Row row, String query) {
    return contains(row.label(), query) || contains(row.id(), query) || contains(row.type(), query);
  }

  private boolean contains(String value, String query) {
    return value != null && value.toLowerCase().contains(query);
  }

  private void openSelection() {
    int selected = table.getSelectedRow();
    if (selected >= 0 && selected < filtered.size()) {
      Row row = filtered.get(selected);
      if (listener != null) {
        listener.onOpen(row);
      }
      setVisible(false);
      dispose();
    }
  }

  private class Model extends AbstractTableModel {
    private final String[] columns = {"Type", "ID", "Libellé"};

    @Override
    public int getRowCount() {
      return filtered.size();
    }

    @Override
    public int getColumnCount() {
      return columns.length;
    }

    @Override
    public String getColumnName(int column) {
      return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      Row row = filtered.get(rowIndex);
      return switch (columnIndex) {
        case 0 -> row.type();
        case 1 -> row.id();
        default -> row.label();
      };
    }
  }
}
