package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import com.location.client.ui.uikit.TableUtils;
import com.location.client.ui.uikit.Toasts;
import com.location.client.ui.uikit.Ui;

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
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
  public static JPanel createContent(DataSourceProvider dsp) {
    ClientsAdminFrame tmp = new ClientsAdminFrame(dsp, true);
    return (JPanel) tmp.getContentPane();
  }

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
  private final JButton btnMerge = new JButton("Fusionner doublons");

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
    this(dsp, false);
  }

  private ClientsAdminFrame(DataSourceProvider dsp, boolean forEmbedding) {
    super("Clients — Administration");
    TableUtils.applySearch(listTable, tfSearch, 250);
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
    toolbar.addSeparator();
    toolbar.add(btnMerge);
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

    btnMerge.addActionListener(e -> mergeSelected());
    btnMerge.setEnabled(false);

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
                updateMergeButtonState();
              }
            });

    setSize(1000, 620);
    if (!forEmbedding) {
      setLocationRelativeTo(null);
    }

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
      Ui.ensure(
          () ->
              JOptionPane.showMessageDialog(
                  this, "Le nom est obligatoire", "Validation", JOptionPane.WARNING_MESSAGE));
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
      Toasts.success(this, "Client enregistré");
    } catch (RuntimeException ex) {
      Toasts.error(this, ex.getMessage());
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
        Toasts.success(this, "Client supprimé");
        newClient();
        refreshList();
      } catch (RuntimeException ex) {
        Toasts.error(this, ex.getMessage());
      }
    }
  }

  private void refreshList() {
    String selectedId = current == null ? null : current.id();
    listModel.setRowCount(0);
    listData.clear();
    List<Models.Client> clients = new ArrayList<>(dsp.listClients());
    clients.sort(Comparator.comparing(Models.Client::name, String.CASE_INSENSITIVE_ORDER));
    for (Models.Client client : clients) {
      listData.add(client);
      listModel.addRow(new Object[] {client.name(), client.email(), client.phone(), client.city()});
    }
    if (selectedId != null) {
      for (int i = 0; i < listData.size(); i++) {
        if (Objects.equals(listData.get(i).id(), selectedId)) {
          final int modelRow = i;
          SwingUtilities.invokeLater(
              () -> {
                int viewRow = listTable.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                  listTable.setRowSelectionInterval(viewRow, viewRow);
                }
              });
          return;
        }
      }
    }
    if (listData.isEmpty()) {
      newClient();
    }
    updateMergeButtonState();
  }

  private void loadSelected() {
    int row = listTable.getSelectedRow();
    if (row < 0) {
      return;
    }
    int modelRow = listTable.convertRowIndexToModel(row);
    if (modelRow < 0 || modelRow >= listData.size()) {
      return;
    }
    Models.Client selected = listData.get(modelRow);
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
    updateMergeButtonState();
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
      Ui.ensure(
          () ->
              JOptionPane.showMessageDialog(
                  this,
                  "Enregistrez d'abord le client",
                  "Information",
                  JOptionPane.INFORMATION_MESSAGE));
      return;
    }
    Models.Contact edited = editContactDialog(null);
    if (edited != null) {
      try {
        dsp.saveContact(edited);
        loadContacts(current.id());
      } catch (RuntimeException ex) {
        Toasts.error(this, ex.getMessage());
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
        Toasts.error(this, ex.getMessage());
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
        Toasts.error(this, ex.getMessage());
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
        Ui.ensure(
            () ->
                JOptionPane.showMessageDialog(
                    this,
                    "Renseignez au moins un prénom ou un nom",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE));
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
      String message = "Export CSV généré : " + tmp.getAbsolutePath();
      Ui.ensure(() -> JOptionPane.showMessageDialog(this, message));
      if (Desktop.isDesktopSupported()) {
        try {
          Desktop.getDesktop().open(tmp.getParentFile());
        } catch (IOException ex) {
          // Ignorer impossibilité d'ouvrir le dossier
        }
      }
    } catch (IOException ex) {
      String message = "Erreur export : " + ex.getMessage();
      Ui.ensure(
          () ->
              JOptionPane.showMessageDialog(
                  this, message, "Erreur", JOptionPane.ERROR_MESSAGE));
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

  private void mergeSelected() {
    int selectedRow = listTable.getSelectedRow();
    if (selectedRow < 0) {
      Ui.ensure(
          () ->
              JOptionPane.showMessageDialog(
                  this,
                  "Sélectionnez d'abord un client",
                  "Fusion",
                  JOptionPane.INFORMATION_MESSAGE));
      return;
    }
    if (listData.size() < 2) {
      Ui.ensure(
          () ->
              JOptionPane.showMessageDialog(
                  this,
                  "Au moins deux clients sont nécessaires pour une fusion",
                  "Fusion",
                  JOptionPane.INFORMATION_MESSAGE));
      return;
    }
    int modelRow = listTable.convertRowIndexToModel(selectedRow);
    if (modelRow < 0 || modelRow >= listData.size()) {
      return;
    }
    Models.Client primary = listData.get(modelRow);
    if (primary.id() == null) {
      Ui.ensure(
          () ->
              JOptionPane.showMessageDialog(
                  this,
                  "Enregistrez le client sélectionné avant de lancer une fusion",
                  "Fusion",
                  JOptionPane.INFORMATION_MESSAGE));
      return;
    }

    List<Models.Client> candidates = new ArrayList<>();
    for (Models.Client client : listData) {
      if (client != primary && client.id() != null) {
        candidates.add(client);
      }
    }
    if (candidates.isEmpty()) {
      Ui.ensure(
          () ->
              JOptionPane.showMessageDialog(
                  this,
                  "Aucun autre client disponible pour la fusion",
                  "Fusion",
                  JOptionPane.INFORMATION_MESSAGE));
      return;
    }

    JComboBox<Models.Client> combo = new JComboBox<>(candidates.toArray(new Models.Client[0]));
    combo.setRenderer(
        new DefaultListCellRenderer() {
          @Override
          public java.awt.Component getListCellRendererComponent(
              javax.swing.JList<?> list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus) {
            JLabel base =
                (JLabel)
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Models.Client client) {
              base.setText(formatClientChoice(client));
            }
            return base;
          }
        });
    int option =
        JOptionPane.showConfirmDialog(
            this,
            combo,
            "Choisissez le client à fusionner",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
    if (option != JOptionPane.OK_OPTION) {
      return;
    }
    Models.Client secondary = (Models.Client) combo.getSelectedItem();
    if (secondary == null) {
      return;
    }

    ClientsMergeDialog dialog = new ClientsMergeDialog(this, primary, secondary);
    dialog.setVisible(true);
    if (!dialog.isOk()) {
      return;
    }

    Models.Client merged = dialog.merged(primary, secondary);
    try {
      Models.Client saved = dsp.saveClient(merged);
      try {
        transferContacts(secondary, saved);
      } catch (UnsupportedOperationException ex) {
        // Certaines implémentations ne gèrent pas les contacts, ignorer silencieusement
      }
      if (secondary.id() != null) {
        dsp.deleteClient(secondary.id());
      }
      current = saved;
      refreshList();
      Toasts.success(this, "Clients fusionnés");
    } catch (RuntimeException ex) {
      Toasts.error(this, ex.getMessage());
    }
  }

  private void transferContacts(Models.Client from, Models.Client to) {
    if (from.id() == null || to.id() == null) {
      return;
    }
    List<Models.Contact> contacts = dsp.listContacts(from.id());
    for (Models.Contact contact : contacts) {
      Models.Contact updated =
          new Models.Contact(
              contact.id(),
              to.id(),
              contact.firstName(),
              contact.lastName(),
              contact.email(),
              contact.phone());
      dsp.saveContact(updated);
    }
  }

  private static String formatClientChoice(Models.Client client) {
    StringBuilder sb = new StringBuilder();
    if (client.name() != null && !client.name().isBlank()) {
      sb.append(client.name());
    } else {
      sb.append("(sans nom)");
    }
    if (client.email() != null && !client.email().isBlank()) {
      sb.append(" — ").append(client.email());
    }
    return sb.toString();
  }

  private void updateMergeButtonState() {
    boolean enabled = listTable.getSelectedRow() >= 0 && listData.size() > 1;
    btnMerge.setEnabled(enabled);
  }
}
