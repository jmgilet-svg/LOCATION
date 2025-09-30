package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import com.location.client.ui.icons.SvgIconLoader;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import com.location.client.ui.uikit.Toasts;

public class ResourceTypeManagerFrame extends JFrame {
  private final DataSourceProvider dataSourceProvider;
  private final DefaultTableModel model =
      new DefaultTableModel(new Object[] {"ID", "Nom", "Icône"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
          return false;
        }
      };
  private final JTable table = new JTable(model);
  private final JTextField tfName = new JTextField(20);
  private final javax.swing.JComboBox<String> cbIcon = new javax.swing.JComboBox<>();
  private final JLabel iconPreview = new JLabel();
  private Models.ResourceType current;
  private boolean supported = true;
  private boolean notifiedUnsupported;

  public ResourceTypeManagerFrame(DataSourceProvider dsp) {
    super("Types de ressources");
    this.dataSourceProvider = dsp;
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(8, 8));

    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setRowHeight(28);
    table.getColumnModel()
        .getColumn(2)
        .setCellRenderer(
            new DefaultTableCellRenderer() {
              @Override
              public Component getTableCellRendererComponent(
                  JTable table,
                  Object value,
                  boolean isSelected,
                  boolean hasFocus,
                  int row,
                  int column) {
                JLabel base =
                    (JLabel)
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String icon = value == null ? null : value.toString();
                base.setText(icon);
                base.setIcon(SvgIconLoader.load(icon, 20));
                return base;
              }
            });
    table.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        onTableSelection();
      }
    });

    JScrollPane scroll = new JScrollPane(table);
    scroll.setPreferredSize(new Dimension(360, 240));

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(new JButton(new AbstractAction("Nouveau") {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        select(null);
      }
    }));
    toolbar.add(new JButton(new AbstractAction("Supprimer") {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        deleteType();
      }
    }));

    JPanel left = new JPanel(new BorderLayout());
    left.add(toolbar, BorderLayout.NORTH);
    left.add(scroll, BorderLayout.CENTER);

    JPanel form = new JPanel(new java.awt.GridBagLayout());
    java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
    c.insets = new Insets(6, 6, 6, 6);
    c.anchor = java.awt.GridBagConstraints.WEST;
    c.gridx = 0;
    c.gridy = 0;
    form.add(new JLabel("Nom"), c);
    c.gridx = 1;
    c.fill = java.awt.GridBagConstraints.HORIZONTAL;
    c.weightx = 1.0;
    form.add(tfName, c);

    c.gridx = 0;
    c.gridy = 1;
    c.weightx = 0;
    c.fill = java.awt.GridBagConstraints.NONE;
    form.add(new JLabel("Icône"), c);
    c.gridx = 1;
    c.fill = java.awt.GridBagConstraints.HORIZONTAL;
    form.add(cbIcon, c);

    c.gridy = 2;
    form.add(iconPreview, c);

    iconPreview.setPreferredSize(new Dimension(64, 64));

    JButton btSave = new JButton(new AbstractAction("Enregistrer") {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        saveType();
      }
    });

    JPanel right = new JPanel(new BorderLayout(8, 8));
    right.add(form, BorderLayout.CENTER);
    right.add(btSave, BorderLayout.SOUTH);

    add(left, BorderLayout.CENTER);
    add(right, BorderLayout.EAST);

    cbIcon.setModel(new DefaultComboBoxModel<>(SvgIconLoader.listAvailable().toArray(new String[0])));
    cbIcon.addActionListener(e -> updatePreview());
    updatePreview();

    setSize(720, 360);
    setLocationRelativeTo(null);
    refresh();
  }

  private void onTableSelection() {
    int row = table.getSelectedRow();
    if (row < 0) {
      select(null);
      return;
    }
    String id = value(row, 0);
    String name = value(row, 1);
    String icon = value(row, 2);
    current = new Models.ResourceType(id, name, icon);
    tfName.setText(name);
    cbIcon.setSelectedItem(icon);
    updatePreview();
  }

  private String value(int row, int column) {
    Object val = model.getValueAt(row, column);
    return val == null ? "" : val.toString();
  }

  private void select(Models.ResourceType type) {
    current = type;
    tfName.setText(type == null ? "" : type.name());
    if (type == null) {
      cbIcon.setSelectedIndex(0);
    } else {
      cbIcon.setSelectedItem(type.iconName());
    }
    table.clearSelection();
    updatePreview();
  }

  private void refresh() {
    if (!supported) {
      return;
    }
    try {
      model.setRowCount(0);
      for (Models.ResourceType type : dataSourceProvider.listResourceTypes()) {
        model.addRow(new Object[] {type.id(), type.name(), type.iconName()});
      }
      if (model.getRowCount() > 0) {
        table.setRowSelectionInterval(0, 0);
      } else {
        select(null);
      }
    } catch (UnsupportedOperationException ex) {
      handleUnsupported(ex);
    }
  }

  private void saveType() {
    if (!supported) {
      return;
    }
    String name = tfName.getText().trim();
    if (name.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Nom obligatoire", "Validation", JOptionPane.WARNING_MESSAGE);
      return;
    }
    String icon = cbIcon.getSelectedItem() == null ? null : cbIcon.getSelectedItem().toString();
    try {
      Models.ResourceType saved =
          dataSourceProvider.saveResourceType(
              new Models.ResourceType(current == null ? null : current.id(), name, icon));
      current = saved;
      refresh();
      Toasts.success(this, "Type enregistré");
    } catch (UnsupportedOperationException ex) {
      handleUnsupported(ex);
    } catch (RuntimeException ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void deleteType() {
    if (!supported) {
      return;
    }
    int row = table.getSelectedRow();
    if (row < 0) {
      return;
    }
    String id = value(row, 0);
    if (id.isBlank()) {
      return;
    }
    int confirm =
        JOptionPane.showConfirmDialog(
            this,
            "Supprimer ce type ? Les ressources associées perdront ce type.",
            "Confirmation",
            JOptionPane.OK_CANCEL_OPTION);
    if (confirm != JOptionPane.OK_OPTION) {
      return;
    }
    try {
      dataSourceProvider.deleteResourceType(id);
      refresh();
      Toasts.info(this, "Type supprimé");
    } catch (UnsupportedOperationException ex) {
      handleUnsupported(ex);
    } catch (RuntimeException ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void updatePreview() {
    String icon = cbIcon.getSelectedItem() == null ? null : cbIcon.getSelectedItem().toString();
    ImageIcon img = SvgIconLoader.load(icon, 48);
    iconPreview.setIcon(img);
  }

  private void handleUnsupported(RuntimeException ex) {
    supported = false;
    table.setEnabled(false);
    tfName.setEnabled(false);
    cbIcon.setEnabled(false);
    if (!notifiedUnsupported) {
      JOptionPane.showMessageDialog(
          this,
          "Gestion des types indisponible pour cette source de données." +
              (ex.getMessage() == null ? "" : "\n" + ex.getMessage()),
          "Information",
          JOptionPane.INFORMATION_MESSAGE);
      notifiedUnsupported = true;
    }
  }
}
