package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Comparator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import com.location.client.ui.uikit.Toasts;

public class ResourceColorDialog extends JDialog {
  private final DataSourceProvider dsp;
  private final PlanningPanel planning;
  private final JComboBox<Models.Resource> resourceCombo = new JComboBox<>();
  private final JPanel preview = new JPanel();
  private final JLabel hexLabel = new JLabel("—");

  public ResourceColorDialog(Window owner, DataSourceProvider dsp, PlanningPanel planning) {
    super(owner, "Couleurs des ressources", ModalityType.APPLICATION_MODAL);
    this.dsp = dsp;
    this.planning = planning;
    setLayout(new BorderLayout(8, 8));

    JLabel intro =
        new JLabel(
            "Sélectionnez une ressource et choisissez une couleur d'affichage pour le planning.");
    intro.setHorizontalAlignment(SwingConstants.LEFT);
    add(intro, BorderLayout.NORTH);

    JPanel center = new JPanel(new BorderLayout(6, 6));
    center.add(resourceCombo, BorderLayout.NORTH);

    preview.setBorder(javax.swing.BorderFactory.createLineBorder(Color.DARK_GRAY));
    preview.setPreferredSize(new java.awt.Dimension(120, 60));
    center.add(preview, BorderLayout.CENTER);

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton pick = new JButton(new PickColorAction());
    JButton reset = new JButton(new ResetColorAction());
    JButton close = new JButton(new CloseAction());
    south.add(new JLabel("Couleur actuelle :"));
    south.add(hexLabel);
    south.add(new JSeparator(SwingConstants.VERTICAL));
    south.add(pick);
    south.add(reset);
    south.add(close);
    add(center, BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);

    resourceCombo.addActionListener(e -> updatePreview());
    loadResources();
    updatePreview();

    pack();
    setLocationRelativeTo(owner);
  }

  private void loadResources() {
    resourceCombo.removeAllItems();
    List<Models.Resource> list = dsp.listResources();
    list.stream()
        .sorted(Comparator.comparing(Models.Resource::name, String.CASE_INSENSITIVE_ORDER))
        .forEach(resourceCombo::addItem);
    if (!list.isEmpty()) {
      resourceCombo.setSelectedIndex(0);
    }
  }

  private void updatePreview() {
    Models.Resource resource = (Models.Resource) resourceCombo.getSelectedItem();
    Color color = ResourceColors.colorFor(resource);
    preview.setBackground(color);
    String override = resource == null ? null : ResourceColors.getOverrideHex(resource.id());
    if (override == null) {
      hexLabel.setText(String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()));
    } else {
      hexLabel.setText(override.toUpperCase());
    }
    preview.repaint();
  }

  private final class PickColorAction extends AbstractAction {
    PickColorAction() {
      super("Choisir couleur…");
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
      Models.Resource resource = (Models.Resource) resourceCombo.getSelectedItem();
      if (resource == null) {
        return;
      }
      Color initial = ResourceColors.colorFor(resource);
      Color chosen = javax.swing.JColorChooser.showDialog(ResourceColorDialog.this, "Couleur", initial);
      if (chosen != null) {
        ResourceColors.setOverride(resource.id(), chosen);
        planning.refreshResourceColors();
        Toasts.success(ResourceColorDialog.this, "Couleur enregistrée");
        updatePreview();
      }
    }
  }

  private final class ResetColorAction extends AbstractAction {
    ResetColorAction() {
      super("Réinitialiser");
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
      Models.Resource resource = (Models.Resource) resourceCombo.getSelectedItem();
      if (resource == null) {
        return;
      }
      ResourceColors.clearOverride(resource.id());
      planning.refreshResourceColors();
      Toasts.info(ResourceColorDialog.this, "Couleur réinitialisée");
      updatePreview();
    }
  }

  private final class CloseAction extends AbstractAction {
    CloseAction() {
      super("Fermer");
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
      dispose();
    }
  }
}
