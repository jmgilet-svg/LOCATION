package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Éditeur rapide pour ajuster une intervention : titre, horaires et ressource.
 */
public class QuickEditDialog extends JDialog {
  public interface SaveListener {
    void onSaved(Models.Intervention saved);
  }

  private final DataSourceProvider dsp;
  private final Models.Intervention base;
  private final List<Models.Resource> resources;
  private final JTextField titleField = new JTextField(24);
  private final JTextField startField = new JTextField(6);
  private final JTextField endField = new JTextField(6);
  private final JComboBox<Models.Resource> resourceCombo;
  private SaveListener listener;

  public QuickEditDialog(
      Window owner,
      DataSourceProvider dsp,
      Models.Intervention base,
      List<Models.Resource> resources) {
    super(owner, "Édition rapide", ModalityType.APPLICATION_MODAL);
    this.dsp = dsp;
    this.base = base;
    this.resources = resources;
    this.resourceCombo = new JComboBox<>(resources.toArray(new Models.Resource[0]));
    initUi();
    pack();
    setLocationRelativeTo(owner);
  }

  public QuickEditDialog onSaved(SaveListener listener) {
    this.listener = listener;
    return this;
  }

  private void initUi() {
    setLayout(new GridBagLayout());
    getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
    GridBagConstraints c = new GridBagConstraints();
    c.anchor = GridBagConstraints.WEST;
    c.insets = new Insets(4, 6, 4, 6);

    titleField.setText(base.title() != null ? base.title() : "");
    startField.setText(formatTime(base.start()));
    endField.setText(formatTime(base.end()));
    resourceCombo.setRenderer(
        new DefaultListCellRenderer() {
          @Override
          public java.awt.Component getListCellRendererComponent(
              javax.swing.JList<?> list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Models.Resource resource) {
              setText(resource.name());
            }
            return this;
          }
        });
    for (int i = 0; i < resourceCombo.getItemCount(); i++) {
      Models.Resource r = resourceCombo.getItemAt(i);
      if (r != null && r.id().equals(base.resourceId())) {
        resourceCombo.setSelectedIndex(i);
        break;
      }
    }

    int row = 0;
    addLabel("Titre", c, row);
    addField(titleField, c, row++);
    addLabel("Début (HH:mm)", c, row);
    addField(startField, c, row++);
    addLabel("Fin (HH:mm)", c, row);
    addField(endField, c, row++);
    addLabel("Ressource", c, row);
    addField(resourceCombo, c, row++);

    JPanel buttons = new JPanel();
    JButton cancel =
        new JButton(
            new AbstractAction("Annuler") {
              @Override
              public void actionPerformed(ActionEvent e) {
                dispose();
              }
            });
    JButton save =
        new JButton(
            new AbstractAction("Enregistrer") {
              @Override
              public void actionPerformed(ActionEvent e) {
                save();
              }
            });
    getRootPane().setDefaultButton(save);
    buttons.add(cancel);
    buttons.add(save);

    c.gridx = 0;
    c.gridy = row;
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.EAST;
    add(buttons, c);
  }

  private void addLabel(String text, GridBagConstraints c, int row) {
    c.gridx = 0;
    c.gridy = row;
    c.gridwidth = 1;
    add(new JLabel(text), c);
  }

  private void addField(java.awt.Component component, GridBagConstraints c, int row) {
    c.gridx = 1;
    c.gridy = row;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(component, c);
    c.weightx = 0;
    c.fill = GridBagConstraints.NONE;
  }

  private String formatTime(Instant instant) {
    return instant
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .truncatedTo(ChronoUnit.MINUTES)
        .toString();
  }

  private void save() {
    try {
      LocalTime start = LocalTime.parse(startField.getText().trim());
      LocalTime end = LocalTime.parse(endField.getText().trim());
      if (!end.isAfter(start)) {
        throw new IllegalArgumentException("L'heure de fin doit être après le début.");
      }
      Models.Resource selectedResource = (Models.Resource) resourceCombo.getSelectedItem();
      if (selectedResource == null) {
        throw new IllegalArgumentException("Sélectionnez une ressource valide.");
      }
      ZoneId zone = ZoneId.systemDefault();
      LocalDate date = base.start().atZone(zone).toLocalDate();
      Instant newStart = date.atTime(start).atZone(zone).toInstant();
      Instant newEnd = date.atTime(end).atZone(zone).toInstant();
      String title = titleField.getText().trim();
      if (title.isBlank()) {
        throw new IllegalArgumentException("Le titre ne peut pas être vide.");
      }
      Models.Intervention payload =
          new Models.Intervention(
              base.id(),
              selectedResource.agencyId(),
              selectedResource.id(),
              base.clientId(),
              base.driverId(),
              title,
              newStart,
              newEnd,
              base.notes());
      Models.Intervention saved = dsp.updateIntervention(payload);
      ActivityCenter.log("Intervention mise à jour " + saved.id());
      Window owner = getOwner();
      if (owner instanceof MainFrame mf) {
        mf.toastSuccess("Modifications enregistrées");
      } else {
        Toast.success(owner, "Modifications enregistrées");
      }
      if (listener != null) {
        listener.onSaved(saved);
      }
      dispose();
    } catch (RuntimeException ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }
}
