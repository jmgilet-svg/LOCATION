package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Administration des clients & contacts. */
public class ClientsAdminFrame extends JFrame {
  private final DataSourceProvider dsp;

  private final DefaultTableModel listModel =
      new DefaultTableModel(new Object[] {"Nom", "Email", "Téléphone", "Ville"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };
  private final JTable listTable = new JTable(listModel);
  private final JTextField tfSearch = new JTextField(24);
  private final List<Models.Client> listData = new ArrayList<>();

  private Models.Client current;
  private final JTextField tfName = new JTextField(24);
  private final JTextField tfEmail = new JTextField(24);
  private final JTextField tfPhone = new JTextField(16);
  private final JTextField tfAddress = new JTextField(24);
  private final JTextField tfZip = new JTextField(8);
  private final JTextField tfCity = new JTextField(16);
  private final JTextField tfVat = new JTextField(18);
  private final JTextField tfIban = new JTextField(26);

  private final DefaultTableModel contactModel =
      new DefaultTableModel(new Object[] {"Prénom", "Nom", "Email", "Téléphone"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };
  private final JTable contactsTable = new JTable(contactModel);
  private final List<Models.Contact> contactData = new ArrayList<>();

  public ClientsAdminFrame(DataSourceProvider dsp) {
    super("Clients — Administration");
    this.dsp = dsp;
    setLayout(new BorderLayout(8, 8));

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(new AbstractAction("Nouveau client") {
      @Override
      public void actionPerformed(ActionEvent e) {
        newClient();
      }
    });
    toolbar.add(new AbstractAction("Enregistrer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        saveClient();
      }
    });
    toolbar.add(new AbstractAction("Supprimer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        deleteClient();
      }
    });
    toolbar.addSeparator();
    toolbar.add(new AbstractAction("Exporter CSV") {
      @Override
      public void actionPerformed(ActionEvent e) {
        exportCsv();
      }
    });
    add(toolbar, BorderLayout.NORTH);

    JPanel left = new JPanel(new BorderLayout(6, 6));
    JPanel searchPanel = new JPanel(new BorderLayout(6, 6));
    searchPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
    searchPanel.add(new JLabel("Recherche"), BorderLayout.WEST);
    searchPanel.add(tfSearch, BorderLayout.CENTER);
    JButton btClear = new JButton("Effacer");
    searchPanel.add(btClear, BorderLayout.EAST);
    btClear.addActionListener(e -> {
      tfSearch.setText("");
      refreshList();
    });
    tfSearch.addActionListener(e -> refreshList());
    left.add(searchPanel, BorderLayout.NORTH);

    listTable.setRowHeight(24);
    listTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    left.add(new JScrollPane(listTable), BorderLayout.CENTER);

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Informations", buildClientForm());
    tabs.addTab("Contacts", buildContactsTab());

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, tabs);
    split.setResizeWeight(0.35);
    split.setDividerLocation(360);
    add(split, BorderLayout.CENTER);

    listTable.getSelectionModel()
        .addListSelectionListener(
            e -> {
              if (!e.getValueIsAdjusting()) {
                loadSelected();
              }
            });

    setSize(1000, 620);
    setLocationRelativeTo(null);

    refreshList();
  }

  private JPanel buildClientForm() {
    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(6, 6, 6, 6);
    c.anchor = GridBagConstraints.WEST;
    int y = 0;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Nom"), c);
    c.gridx = 1;
    form.add(tfName, c);
    y++;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Email"), c);
    c.gridx = 1;
    form.add(tfEmail, c);
    y++;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Téléphone"), c);
    c.gridx = 1;
    form.add(tfPhone, c);
    y++;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Adresse"), c);
    c.gridx = 1;
    form.add(tfAddress, c);
    y++;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("CP"), c);
    c.gridx = 1;
    form.add(tfZip, c);
    y++;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Ville"), c);
    c.gridx = 1;
    form.add(tfCity, c);
    y++;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("N° TVA"), c);
    c.gridx = 1;
    form.add(tfVat, c);
    y++;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("IBAN"), c);
    c.gridx = 1;
    form.add(tfIban, c);
    return form;
  }

  private JPanel buildContactsTab() {
    JPanel panel = new JPanel(new BorderLayout(6, 6));
    contactsTable.setRowHeight(22);
    contactsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    panel.add(new JScrollPane(contactsTable), BorderLayout.CENTER);

    JToolBar tb = new JToolBar();
    tb.setFloatable(false);
    tb.add(new AbstractAction("Ajouter") {
      @Override
      public void actionPerformed(ActionEvent e) {
        addContact();
      }
    });
    tb.add(new AbstractAction("Modifier") {
      @Override
      public void actionPerformed(ActionEvent e) {
        editContact();
      }
    });
    tb.add(new AbstractAction("Supprimer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        deleteContact();
      }
    });
    panel.add(tb, BorderLayout.NORTH);
    return panel;
  }

  private void newClient() {
    current = null;
    tfName.setText("");
    tfEmail.setText("");
    tfPhone.setText("");
    tfAddress.setText("");
    tfZip.setText("");
    tfCity.setText("");
    tfVat.setText("");
    tfIban.setText("");
    contactModel.setRowCount(0);
    contactData.clear();
    listTable.clearSelection();
  }

  private void saveClient() {
    String name = tfName.getText().trim();
    if (name.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Le nom est obligatoire", "Validation", JOptionPane.WARNING_MESSAGE);
      return;
    }
    Models.Client payload =
        new Models.Client(
            current == null ? null : current.id(),
            name,
            blankToNull(tfEmail.getText()),
            blankToNull(tfPhone.getText()),
            blankToNull(tfAddress.getText()),
            blankToNull(tfZip.getText()),
            blankToNull(tfCity.getText()),
            blankToNull(tfVat.getText()),
            blankToNull(tfIban.getText()));
    try {
      current = dsp.saveClient(payload);
      refreshList();
      Toast.success(this, "Client enregistré");
    } catch (RuntimeException ex) {
      Toast.error(this, ex.getMessage());
    }
  }

  private void deleteClient() {
    if (current == null) {
      return;
    }
    int confirm =
        JOptionPane.showConfirmDialog(
            this,
            "Supprimer ce client ?",
            "Confirmation",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
    if (confirm == JOptionPane.OK_OPTION) {
      try {
        dsp.deleteClient(current.id());
        Toast.success(this, "Client supprimé");
        newClient();
        refreshList();
      } catch (RuntimeException ex) {
        Toast.error(this, ex.getMessage());
      }
    }
  }

  private void refreshList() {
    String query = tfSearch.getText().trim().toLowerCase();
    String selectedId = current == null ? null : current.id();
    listModel.setRowCount(0);
    listData.clear();
    List<Models.Client> clients = new ArrayList<>(dsp.listClients());
    clients.sort(Comparator.comparing(Models.Client::name, String.CASE_INSENSITIVE_ORDER));
    for (Models.Client client : clients) {
      if (matchesQuery(client, query)) {
        listData.add(client);
        listModel.addRow(new Object[] {client.name(), client.email(), client.phone(), client.city()});
      }
    }
    if (selectedId != null) {
      for (int i = 0; i < listData.size(); i++) {
        if (Objects.equals(listData.get(i).id(), selectedId)) {
          final int row = i;
          SwingUtilities.invokeLater(() -> listTable.setRowSelectionInterval(row, row));
          return;
        }
      }
    }
    if (listData.isEmpty()) {
      newClient();
    }
  }

  private boolean matchesQuery(Models.Client client, String query) {
    if (query.isEmpty()) {
      return true;
    }
    return (client.name() != null && client.name().toLowerCase().contains(query))
        || (client.phone() != null && client.phone().toLowerCase().contains(query))
        || (client.city() != null && client.city().toLowerCase().contains(query))
        || (client.email() != null && client.email().toLowerCase().contains(query));
  }

  private void loadSelected() {
    int row = listTable.getSelectedRow();
    if (row < 0 || row >= listData.size()) {
      return;
    }
    Models.Client selected = listData.get(row);
    current = selected;
    tfName.setText(selected.name() == null ? "" : selected.name());
    tfEmail.setText(selected.email() == null ? "" : selected.email());
    tfPhone.setText(selected.phone() == null ? "" : selected.phone());
    tfAddress.setText(selected.address() == null ? "" : selected.address());
    tfZip.setText(selected.zip() == null ? "" : selected.zip());
    tfCity.setText(selected.city() == null ? "" : selected.city());
    tfVat.setText(selected.vatNumber() == null ? "" : selected.vatNumber());
    tfIban.setText(selected.iban() == null ? "" : selected.iban());
    loadContacts(selected.id());
  }

  private void loadContacts(String clientId) {
    contactModel.setRowCount(0);
    contactData.clear();
    if (clientId == null || clientId.isBlank()) {
      return;
    }
    for (Models.Contact contact : dsp.listContacts(clientId)) {
      contactData.add(contact);
      contactModel.addRow(
          new Object[] {
            contact.firstName(), contact.lastName(), contact.email(), contact.phone()
          });
    }
  }

  private void addContact() {
    if (current == null || current.id() == null) {
      JOptionPane.showMessageDialog(
          this, "Enregistrez d'abord le client", "Information", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    Models.Contact edited = editContactDialog(null);
    if (edited != null) {
      try {
        dsp.saveContact(edited);
        loadContacts(current.id());
      } catch (RuntimeException ex) {
        Toast.error(this, ex.getMessage());
      }
    }
  }

  private void editContact() {
    if (current == null || current.id() == null) {
      return;
    }
    int row = contactsTable.getSelectedRow();
    if (row < 0 || row >= contactData.size()) {
      return;
    }
    Models.Contact base = contactData.get(row);
    Models.Contact edited = editContactDialog(base);
    if (edited != null) {
      try {
        dsp.saveContact(edited);
        loadContacts(current.id());
      } catch (RuntimeException ex) {
        Toast.error(this, ex.getMessage());
      }
    }
  }

  private void deleteContact() {
    if (current == null || current.id() == null) {
      return;
    }
    int row = contactsTable.getSelectedRow();
    if (row < 0 || row >= contactData.size()) {
      return;
    }
    Models.Contact contact = contactData.get(row);
    int confirm =
        JOptionPane.showConfirmDialog(
            this,
            "Supprimer ce contact ?",
            "Confirmation",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
    if (confirm == JOptionPane.OK_OPTION) {
      try {
        if (contact.id() != null) {
          dsp.deleteContact(contact.id());
        }
        loadContacts(current.id());
      } catch (RuntimeException ex) {
        Toast.error(this, ex.getMessage());
      }
    }
  }

  private Models.Contact editContactDialog(Models.Contact base) {
    JTextField fn = new JTextField(base == null ? "" : valueOrEmpty(base.firstName()), 16);
    JTextField ln = new JTextField(base == null ? "" : valueOrEmpty(base.lastName()), 16);
    JTextField em = new JTextField(base == null ? "" : valueOrEmpty(base.email()), 22);
    JTextField ph = new JTextField(base == null ? "" : valueOrEmpty(base.phone()), 16);

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(4, 6, 4, 6);
    c.anchor = GridBagConstraints.WEST;
    int y = 0;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Prénom"), c);
    c.gridx = 1;
    form.add(fn, c);
    y++;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Nom"), c);
    c.gridx = 1;
    form.add(ln, c);
    y++;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Email"), c);
    c.gridx = 1;
    form.add(em, c);
    y++;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Téléphone"), c);
    c.gridx = 1;
    form.add(ph, c);

    int result =
        JOptionPane.showConfirmDialog(
            this,
            form,
            base == null ? "Nouveau contact" : "Modifier le contact",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
    if (result == JOptionPane.OK_OPTION) {
      if (fn.getText().trim().isEmpty() && ln.getText().trim().isEmpty()) {
        JOptionPane.showMessageDialog(
            this, "Renseignez au moins un prénom ou un nom", "Validation", JOptionPane.WARNING_MESSAGE);
        return null;
      }
      return new Models.Contact(
          base == null ? null : base.id(),
          current.id(),
          blankToNull(fn.getText()),
          blankToNull(ln.getText()),
          blankToNull(em.getText()),
          blankToNull(ph.getText()));
    }
    return null;
  }

  private void exportCsv() {
    try {
      File tmp = File.createTempFile("clients-", ".csv");
      CsvUtil.exportClients(dsp, tmp.toPath());
      JOptionPane.showMessageDialog(this, "Export CSV généré : " + tmp.getAbsolutePath());
      if (Desktop.isDesktopSupported()) {
        try {
          Desktop.getDesktop().open(tmp.getParentFile());
        } catch (IOException ex) {
          // Ignorer impossibilité d'ouvrir le dossier
        }
      }
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Erreur export : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }
}
