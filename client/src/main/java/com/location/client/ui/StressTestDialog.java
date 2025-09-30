package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import com.location.client.ui.uikit.Toasts;

public class StressTestDialog extends JDialog {
  private final DataSourceProvider dsp;
  private final PlanningPanel planning;
  private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 5000, 10));
  private final JSpinner startSpinner = new JSpinner(new SpinnerNumberModel(6, 0, 23, 1));
  private final JSpinner endSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 23, 1));
  private final JSpinner daysSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 30, 1));
  private final JCheckBox todayCheckbox = new JCheckBox("Inclure jour courant", true);

  public StressTestDialog(Window owner, DataSourceProvider dsp, PlanningPanel planning) {
    super(owner, "Générer des interventions", ModalityType.APPLICATION_MODAL);
    this.dsp = dsp;
    this.planning = planning;
    setLayout(new BorderLayout(8, 8));

    JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
    form.add(new JLabel("Nombre d'interventions"));
    form.add(countSpinner);
    form.add(new JLabel("Heure de début min"));
    form.add(startSpinner);
    form.add(new JLabel("Heure de fin max"));
    form.add(endSpinner);
    form.add(new JLabel("Répartir sur N jours"));
    form.add(daysSpinner);
    form.add(new JLabel(""));
    form.add(todayCheckbox);
    add(form, BorderLayout.CENTER);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttons.add(new JButton(new CancelAction()));
    buttons.add(new JButton(new GenerateAction()));
    add(buttons, BorderLayout.SOUTH);

    pack();
    setLocationRelativeTo(owner);
  }

  private final class CancelAction extends AbstractAction {
    CancelAction() {
      super("Annuler");
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
      dispose();
    }
  }

  private final class GenerateAction extends AbstractAction {
    GenerateAction() {
      super("Générer");
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
      List<Models.Resource> resources = dsp.listResources();
      if (resources.isEmpty()) {
        JOptionPane.showMessageDialog(
            StressTestDialog.this,
            "Aucune ressource disponible.",
            "Génération",
            JOptionPane.WARNING_MESSAGE);
        return;
      }
      List<Models.Client> clients = dsp.listClients();
      int requested = (Integer) countSpinner.getValue();
      int startHour = (Integer) startSpinner.getValue();
      int endHour = (Integer) endSpinner.getValue();
      if (endHour <= startHour) {
        JOptionPane.showMessageDialog(
            StressTestDialog.this,
            "L'heure de fin doit être strictement supérieure à l'heure de début.",
            "Génération",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      int spread = (Integer) daysSpinner.getValue();
      LocalDate base = todayCheckbox.isSelected() ? planning.getDay() : planning.getDay().plusDays(1);
      Random random = new Random();
      int created = 0;
      for (int i = 0; i < requested; i++) {
        Models.Resource resource = resources.get(random.nextInt(resources.size()));
        LocalDate day = base.plusDays(random.nextInt(spread));
        int hour = startHour + random.nextInt(Math.max(1, endHour - startHour));
        int durationHours = 1 + random.nextInt(3);
        java.time.Instant start =
            day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant();
        java.time.Instant end = start.plus(Duration.ofHours(durationHours));
        String clientId = clients.isEmpty() ? null : clients.get(random.nextInt(clients.size())).id();
        String title = "Job #" + (1000 + random.nextInt(9000));
        Models.Intervention payload =
            new Models.Intervention(
                null,
                resource.agencyId(),
                resource.id(),
                clientId,
                null,
                title,
                start,
                end,
                null);
        try {
          dsp.createIntervention(payload);
          created++;
        } catch (RuntimeException ex) {
          // On ignore les erreurs unitaires (conflits, validations REST, etc.).
        }
      }
      planning.reload();
      Toasts.success(StressTestDialog.this, created + " interventions générées");
      dispose();
    }
  }
}
