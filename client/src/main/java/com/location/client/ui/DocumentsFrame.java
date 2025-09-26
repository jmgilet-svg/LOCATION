package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class DocumentsFrame extends JFrame {
  private final DataSourceProvider dataSource;
  private final List<Models.Agency> agencies;
  private final List<Models.Client> clients;
  private final Map<String, String> clientNames = new LinkedHashMap<>();

  private final DocTableModel tableModel = new DocTableModel();
  private final JTable table = new JTable(tableModel);
  private final JComboBox<String> typeFilter = new JComboBox<>();
  private final JComboBox<Models.Client> clientFilter = new JComboBox<>();

  public DocumentsFrame(DataSourceProvider dataSource) {
    super("Documents commerciaux");
    this.dataSource = dataSource;
    this.agencies = dataSource.listAgencies();
    this.clients = dataSource.listClients();
    for (Models.Client client : clients) {
      clientNames.put(client.id(), client.name());
    }

    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setPreferredSize(new Dimension(960, 600));
    setLayout(new BorderLayout());

    add(buildFilters(), BorderLayout.NORTH);
    add(buildTable(), BorderLayout.CENTER);
    add(buildToolbar(), BorderLayout.SOUTH);

    pack();
    setLocationRelativeTo(null);
    reload();
  }

  private JPanel buildFilters() {
    JPanel filters = new JPanel();

    typeFilter.setModel(new DefaultComboBoxModel<>(new String[] {"", "QUOTE", "ORDER", "DELIVERY", "INVOICE"}));
    filters.add(new JLabel("Type:"));
    filters.add(typeFilter);

    DefaultComboBoxModel<Models.Client> clientModel = new DefaultComboBoxModel<>();
    clientModel.addElement(null);
    for (Models.Client client : clients) {
      clientModel.addElement(client);
    }
    clientFilter.setModel(clientModel);
    clientFilter.setRenderer(
        new DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(
              JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Models.Client client) {
              setText(client.name());
            } else if (value == null) {
              setText("(Tous les clients)");
            }
            return this;
          }
        });
    filters.add(new JLabel("Client:"));
    filters.add(clientFilter);

    JButton apply = new JButton(new AbstractAction("Filtrer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        reload();
      }
    });
    filters.add(apply);

    return filters;
  }

  private JScrollPane buildTable() {
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setAutoCreateRowSorter(true);
    return new JScrollPane(table);
  }

  private JToolBar buildToolbar() {
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);

    toolbar.add(new AbstractAction("Nouveau devis") {
      @Override
      public void actionPerformed(ActionEvent e) {
        createDocument("QUOTE");
      }
    });
    toolbar.add(new AbstractAction("Dupliquer → Commande") {
      @Override
      public void actionPerformed(ActionEvent e) {
        convertDocument("ORDER");
      }
    });
    toolbar.add(new AbstractAction("Dupliquer → BL") {
      @Override
      public void actionPerformed(ActionEvent e) {
        convertDocument("DELIVERY");
      }
    });
    toolbar.add(new AbstractAction("Dupliquer → Facture") {
      @Override
      public void actionPerformed(ActionEvent e) {
        convertDocument("INVOICE");
      }
    });
    toolbar.addSeparator();
    toolbar.add(new AbstractAction("Éditer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        editDocument();
      }
    });
    toolbar.add(new AbstractAction("Supprimer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        deleteDocument();
      }
    });
    toolbar.addSeparator();
    toolbar.add(new AbstractAction("PDF") {
      @Override
      public void actionPerformed(ActionEvent e) {
        exportPdf();
      }
    });
    toolbar.add(new AbstractAction("Email") {
      @Override
      public void actionPerformed(ActionEvent e) {
        emailDocument();
      }
    });

    return toolbar;
  }

  private void reload() {
    String type = (String) typeFilter.getSelectedItem();
    Models.Client client = (Models.Client) clientFilter.getSelectedItem();
    String clientId = client == null ? null : client.id();
    List<Models.Doc> docs = dataSource.listDocs(type, clientId);
    tableModel.setDocuments(docs);
  }

  private Models.Doc selectedDocument() {
    int viewRow = table.getSelectedRow();
    if (viewRow < 0) {
      return null;
    }
    int modelRow = table.convertRowIndexToModel(viewRow);
    return tableModel.get(modelRow);
  }

  private void createDocument(String type) {
    if (agencies.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Aucune agence disponible.", "Erreur", JOptionPane.ERROR_MESSAGE);
      return;
    }
    Models.Client client = (Models.Client) clientFilter.getSelectedItem();
    if (client == null) {
      JOptionPane.showMessageDialog(this, "Sélectionnez un client avant de créer un document.", "Information", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    String title = JOptionPane.showInputDialog(this, "Titre du document", "Nouveau " + type, JOptionPane.QUESTION_MESSAGE);
    if (title == null || title.isBlank()) {
      return;
    }
    String agencyId = dataSource.getCurrentAgencyId();
    if (agencyId == null || agencyId.isBlank()) {
      JOptionPane.showMessageDialog(
          this,
          "Aucune agence active n'est sélectionnée.",
          "Agence requise",
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    Models.Doc doc = dataSource.createDoc(type, agencyId, client.id(), title.trim());
    tableModel.add(doc);
    SwingUtilities.invokeLater(() -> {
      int row = table.convertRowIndexToView(tableModel.indexOf(doc));
      if (row >= 0) {
        table.getSelectionModel().setSelectionInterval(row, row);
      }
    });
  }

  private void convertDocument(String toType) {
    Models.Doc doc = selectedDocument();
    if (doc == null) {
      return;
    }
    try {
      Models.Doc copy = dataSource.transitionDoc(doc.id(), toType);
      tableModel.add(copy);
      JOptionPane.showMessageDialog(this, "Document converti en " + toType + ".");
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Erreur de conversion : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void editDocument() {
    Models.Doc doc = selectedDocument();
    if (doc == null) {
      return;
    }

    JTextFieldPanel panel = new JTextFieldPanel(doc.reference(), doc.title(), doc.lines());
    int result = JOptionPane.showConfirmDialog(this, panel, "Éditer le document", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }

    String title = panel.title();
    if (title.isBlank()) {
      JOptionPane.showMessageDialog(this, "Le titre est obligatoire.", "Validation", JOptionPane.WARNING_MESSAGE);
      return;
    }

    Models.Doc updatedRequest =
        new Models.Doc(
            doc.id(),
            doc.type(),
            doc.status(),
            panel.reference(),
            title,
            doc.agencyId(),
            doc.clientId(),
            doc.date(),
            doc.totalHt(),
            doc.totalVat(),
            doc.totalTtc(),
            panel.lines());
    try {
      Models.Doc saved = dataSource.updateDoc(updatedRequest);
      tableModel.replace(saved);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Échec de la mise à jour : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void deleteDocument() {
    Models.Doc doc = selectedDocument();
    if (doc == null) {
      return;
    }
    int confirm = JOptionPane.showConfirmDialog(this, "Supprimer le document ?", "Confirmation", JOptionPane.YES_NO_OPTION);
    if (confirm == JOptionPane.YES_OPTION) {
      dataSource.deleteDoc(doc.id());
      tableModel.remove(doc.id());
    }
  }

  private void exportPdf() {
    Models.Doc doc = selectedDocument();
    if (doc == null) {
      return;
    }
    try {
      java.nio.file.Path tmp = Files.createTempFile(doc.type().toLowerCase() + "-" + doc.id() + "-", ".pdf");
      dataSource.downloadDocPdf(doc.id(), tmp);
      if (java.awt.Desktop.isDesktopSupported()) {
        java.awt.Desktop.getDesktop().open(tmp.toFile());
      } else {
        JOptionPane.showMessageDialog(this, "PDF exporté : " + tmp.toAbsolutePath());
      }
    } catch (UnsupportedOperationException ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage(), "Information", JOptionPane.INFORMATION_MESSAGE);
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Impossible d'ouvrir le PDF : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Export PDF impossible : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void emailDocument() {
    Models.Doc doc = selectedDocument();
    if (doc == null) {
      return;
    }
    String to = JOptionPane.showInputDialog(this, "Destinataire", "Envoyer par email", JOptionPane.QUESTION_MESSAGE);
    if (to == null || to.isBlank()) {
      return;
    }
    try {
      String subject = doc.type() + (doc.reference() == null ? "" : " " + doc.reference());
      dataSource.emailDoc(doc.id(), to.trim(), subject, "Bonjour,\nVeuillez trouver le document en pièce jointe.");
      JOptionPane.showMessageDialog(this, "Email envoyé (ou simulé en mode Mock).");
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Échec de l'envoi : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private class DocTableModel extends AbstractTableModel {
    private final String[] columns = {"Type", "Référence", "Titre", "Client", "Date", "HT", "TVA", "TTC"};
    private final List<Models.Doc> documents = new ArrayList<>();

    @Override
    public int getRowCount() {
      return documents.size();
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
      Models.Doc doc = documents.get(rowIndex);
      return switch (columnIndex) {
        case 0 -> doc.type();
        case 1 -> doc.reference();
        case 2 -> doc.title();
        case 3 -> clientNames.getOrDefault(doc.clientId(), doc.clientId());
        case 4 -> doc.date().atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
        case 5 -> doc.totalHt();
        case 6 -> doc.totalVat();
        case 7 -> doc.totalTtc();
        default -> "";
      };
    }

    void setDocuments(List<Models.Doc> docs) {
      documents.clear();
      documents.addAll(docs);
      fireTableDataChanged();
    }

    Models.Doc get(int row) {
      return documents.get(row);
    }

    void add(Models.Doc doc) {
      documents.add(0, doc);
      fireTableDataChanged();
    }

    void replace(Models.Doc updated) {
      for (int i = 0; i < documents.size(); i++) {
        if (Objects.equals(documents.get(i).id(), updated.id())) {
          documents.set(i, updated);
          fireTableRowsUpdated(i, i);
          return;
        }
      }
      add(updated);
    }

    void remove(String id) {
      documents.removeIf(doc -> Objects.equals(doc.id(), id));
      fireTableDataChanged();
    }

    int indexOf(Models.Doc doc) {
      return documents.indexOf(doc);
    }
  }

  private static class JTextFieldPanel extends JPanel {
    private final javax.swing.JTextField referenceField = new javax.swing.JTextField();
    private final javax.swing.JTextField titleField = new javax.swing.JTextField();
    private final DefaultListModel<Models.DocLine> lineModel = new DefaultListModel<>();
    private final JList<Models.DocLine> lineList = new JList<>(lineModel);

    JTextFieldPanel(String reference, String title, List<Models.DocLine> lines) {
      super(new BorderLayout(6, 6));
      referenceField.setText(reference == null ? "" : reference);
      titleField.setText(title);

      JPanel fields = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
      fields.add(new JLabel("Référence:"));
      fields.add(referenceField);
      fields.add(new JLabel("Titre:"));
      fields.add(titleField);
      add(fields, BorderLayout.NORTH);

      if (lines != null) {
        for (Models.DocLine line : lines) {
          lineModel.addElement(line);
        }
      }
      lineList.setVisibleRowCount(6);
      add(new JScrollPane(lineList), BorderLayout.CENTER);
      add(buildLineButtons(), BorderLayout.SOUTH);
    }

    private JPanel buildLineButtons() {
      JPanel panel = new JPanel();
      panel.add(new JButton(new AbstractAction("Ajouter") {
        @Override
        public void actionPerformed(ActionEvent e) {
          LineEditor editor = new LineEditor(null);
          Models.DocLine line = editor.edit();
          if (line != null) {
            lineModel.addElement(line);
          }
        }
      }));
      panel.add(new JButton(new AbstractAction("Modifier") {
        @Override
        public void actionPerformed(ActionEvent e) {
          int index = lineList.getSelectedIndex();
          if (index < 0) {
            return;
          }
          LineEditor editor = new LineEditor(lineModel.get(index));
          Models.DocLine line = editor.edit();
          if (line != null) {
            lineModel.set(index, line);
          }
        }
      }));
      panel.add(new JButton(new AbstractAction("Supprimer") {
        @Override
        public void actionPerformed(ActionEvent e) {
          int index = lineList.getSelectedIndex();
          if (index >= 0) {
            lineModel.remove(index);
          }
        }
      }));
      return panel;
    }

    String reference() {
      String ref = referenceField.getText().trim();
      return ref.isEmpty() ? null : ref;
    }

    String title() {
      return titleField.getText().trim();
    }

    List<Models.DocLine> lines() {
      List<Models.DocLine> lines = new ArrayList<>();
      for (int i = 0; i < lineModel.getSize(); i++) {
        lines.add(lineModel.get(i));
      }
      return List.copyOf(lines);
    }
  }

  private static class LineEditor extends JDialog {
    private Models.DocLine line;
    private final javax.swing.JTextField designation = new javax.swing.JTextField();
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 1000000.0, 0.1));
    private final JSpinner unitPrice = new JSpinner(new SpinnerNumberModel(100.0, 0.0, 1000000.0, 1.0));
    private final JSpinner vatRate = new JSpinner(new SpinnerNumberModel(20.0, 0.0, 100.0, 0.5));

    LineEditor(Models.DocLine base) {
      super((JFrame) null, "Ligne", true);
      setLayout(new BorderLayout(6, 6));

      JPanel grid = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
      grid.add(new JLabel("Désignation:"));
      grid.add(designation);
      grid.add(new JLabel("Quantité:"));
      grid.add(quantity);
      grid.add(new JLabel("PU:"));
      grid.add(unitPrice);
      grid.add(new JLabel("TVA %:"));
      grid.add(vatRate);
      add(grid, BorderLayout.CENTER);

      if (base != null) {
        designation.setText(base.designation());
        quantity.setValue(base.quantity());
        unitPrice.setValue(base.unitPrice());
        vatRate.setValue(base.vatRate());
      }

      JPanel buttons = new JPanel();
      buttons.add(new JButton(new AbstractAction("OK") {
        @Override
        public void actionPerformed(ActionEvent e) {
          line = new Models.DocLine(
              designation.getText().trim(),
              ((Number) quantity.getValue()).doubleValue(),
              ((Number) unitPrice.getValue()).doubleValue(),
              ((Number) vatRate.getValue()).doubleValue());
          dispose();
        }
      }));
      buttons.add(new JButton(new AbstractAction("Annuler") {
        @Override
        public void actionPerformed(ActionEvent e) {
          line = null;
          dispose();
        }
      }));
      add(buttons, BorderLayout.SOUTH);

      pack();
      setLocationRelativeTo(null);
    }

    Models.DocLine edit() {
      setVisible(true);
      return line;
    }
  }
}
