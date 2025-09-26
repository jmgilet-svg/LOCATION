package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.table.DefaultTableModel;

public class ResourceEditorFrame extends JFrame {
  private final DataSourceProvider dataSourceProvider;
  private final DefaultTableModel model;
  private final JTable table;
  private final Map<String, Models.Resource> originals = new HashMap<>();
  private final Map<String, String> agencyNames = new HashMap<>();

  public ResourceEditorFrame(DataSourceProvider dsp) {
    super("Ressources");
    this.dataSourceProvider = dsp;
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(6, 6));

    model =
        new DefaultTableModel(
            new Object[] {"ID", "Nom", "Plaque", "Capacité (t)", "Tags", "Agence"},
            0) {
          @Override
          public boolean isCellEditable(int row, int column) {
            return column != 0 && column != 5;
          }
        };
    table = new JTable(model);
    table.setPreferredScrollableViewportSize(new Dimension(840, 360));
    add(new JScrollPane(table), BorderLayout.CENTER);

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(
        new JButton(
            new AbstractAction("Ajouter") {
              @Override
              public void actionPerformed(ActionEvent e) {
                addRow();
              }
            }));
    toolbar.add(
        new JButton(
            new AbstractAction("Enregistrer") {
              @Override
              public void actionPerformed(ActionEvent e) {
                saveAll();
              }
            }));
    add(toolbar, BorderLayout.NORTH);

    setSize(900, 480);
    setLocationRelativeTo(null);

    refresh();
  }

  private void refresh() {
    originals.clear();
    agencyNames.clear();
    model.setRowCount(0);
    List<Models.Agency> agencies = dataSourceProvider.listAgencies();
    for (Models.Agency agency : agencies) {
      agencyNames.put(agency.id(), agency.name());
    }
    List<Models.Resource> resources = dataSourceProvider.listResources();
    for (Models.Resource resource : resources) {
      originals.put(resource.id(), resource);
      model.addRow(
          new Object[] {
            resource.id(),
            resource.name(),
            resource.licensePlate(),
            resource.capacityTons() == null ? "" : resource.capacityTons(),
            resource.tags(),
            agencyNames.getOrDefault(resource.agencyId(), resource.agencyId())
          });
    }
  }

  private void addRow() {
    String agencyId = dataSourceProvider.getCurrentAgencyId();
    String agencyLabel = agencyId == null ? "" : agencyNames.getOrDefault(agencyId, agencyId);
    model.addRow(new Object[] {null, "Nouvelle ressource", "", "", "", agencyLabel});
  }

  private void saveAll() {
    boolean updated = false;
    for (int i = 0; i < model.getRowCount(); i++) {
      String idRaw = value(model.getValueAt(i, 0));
      String id = idRaw.isBlank() ? null : idRaw.trim();
      String name = value(model.getValueAt(i, 1)).trim();
      String plate = value(model.getValueAt(i, 2)).trim();
      String capacityRaw = value(model.getValueAt(i, 3));
      String tags = value(model.getValueAt(i, 4)).trim();
      Integer capacity = null;
      if (!capacityRaw.isBlank()) {
        try {
          capacity = Integer.parseInt(capacityRaw.trim());
        } catch (NumberFormatException ex) {
          Toast.error(this, "Capacité invalide (ligne " + (i + 1) + ")");
          table.setRowSelectionInterval(i, i);
          table.editCellAt(i, 3);
          return;
        }
      }
      Models.Resource previous = id == null ? null : originals.get(id);
      String agencyId = previous != null ? previous.agencyId() : dataSourceProvider.getCurrentAgencyId();
      Integer color = previous != null ? previous.colorRgb() : null;
      try {
        Models.Resource saved =
            dataSourceProvider.saveResource(
                new Models.Resource(id, name, plate, color, agencyId, tags, capacity));
        model.setValueAt(saved.id(), i, 0);
        model.setValueAt(agencyNames.getOrDefault(saved.agencyId(), saved.agencyId()), i, 5);
        originals.put(saved.id(), saved);
        updated = true;
      } catch (RuntimeException ex) {
        Toast.error(this, "Erreur sauvegarde: " + ex.getMessage());
        table.setRowSelectionInterval(i, i);
        return;
      }
    }
    if (updated) {
      Toast.success(this, "Ressources enregistrées");
      refresh();
    }
  }

  private String value(Object obj) {
    return obj == null ? "" : obj.toString();
  }
}
