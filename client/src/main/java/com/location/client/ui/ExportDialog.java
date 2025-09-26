package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

public class ExportDialog extends JDialog {
  private final JComboBox<String> typeCombo =
      new JComboBox<>(new String[] {"Clients", "Ressources", "Interventions"});
  private final JSpinner fromSpinner = createDateSpinner(LocalDate.now().minusDays(7));
  private final JSpinner toSpinner = createDateSpinner(LocalDate.now().plusDays(1));
  private final DataSourceProvider dataSourceProvider;

  public ExportDialog(MainFrame owner, DataSourceProvider dataSourceProvider) {
    super(owner, "Exporter (CSV)", true);
    this.dataSourceProvider = dataSourceProvider;
    setLayout(new BorderLayout(8, 8));

    JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
    form.add(new JLabel("Type :"));
    form.add(typeCombo);
    form.add(new JLabel("Depuis :"));
    form.add(fromSpinner);
    form.add(new JLabel("Jusqu'à :"));
    form.add(toSpinner);
    add(form, BorderLayout.CENTER);

    typeCombo.addActionListener(e -> toggleDates());
    toggleDates();

    JPanel actions = new JPanel();
    actions.add(new JButton(new AbstractAction("Exporter") {
      @Override
      public void actionPerformed(ActionEvent e) {
        performExport();
      }
    }));
    actions.add(new JButton(new AbstractAction("Fermer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    }));
    add(actions, BorderLayout.SOUTH);

    setPreferredSize(new Dimension(420, 180));
    pack();
    setLocationRelativeTo(owner);
  }

  private void toggleDates() {
    boolean interventionsSelected = "Interventions".equals(typeCombo.getSelectedItem());
    fromSpinner.setEnabled(interventionsSelected);
    toSpinner.setEnabled(interventionsSelected);
  }

  private static JSpinner createDateSpinner(LocalDate date) {
    SpinnerDateModel model = new SpinnerDateModel();
    JSpinner spinner = new JSpinner(model);
    spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
    java.util.Calendar calendar = java.util.Calendar.getInstance();
    calendar.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth(), 0, 0, 0);
    calendar.set(java.util.Calendar.MILLISECOND, 0);
    model.setValue(calendar.getTime());
    return spinner;
  }

  private LocalDate spinnerValue(JSpinner spinner) {
    java.util.Date value = (java.util.Date) spinner.getValue();
    return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private void performExport() {
    try {
      String selection = (String) typeCombo.getSelectedItem();
      java.nio.file.Path tempFile = Files.createTempFile("location-export-", ".csv");
      tempFile.toFile().deleteOnExit();
      if ("Clients".equals(selection)) {
        tempFile = dataSourceProvider.downloadClientsCsv(tempFile);
      } else if ("Ressources".equals(selection)) {
        tempFile = dataSourceProvider.downloadResourcesCsv(null, tempFile);
      } else {
        var from =
            spinnerValue(fromSpinner)
                .atStartOfDay(ZoneId.systemDefault())
                .toOffsetDateTime();
        var to =
            spinnerValue(toSpinner)
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toOffsetDateTime();
        tempFile = dataSourceProvider.downloadInterventionsCsv(from, to, tempFile);
      }
      java.awt.Desktop.getDesktop().open(tempFile.toFile());
    } catch (UnsupportedOperationException ex) {
      JOptionPane.showMessageDialog(
          this,
          "Export CSV indisponible en mode Mock.",
          "Information",
          JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(
          this,
          "Échec export : " + ex.getMessage(),
          "Erreur",
          JOptionPane.ERROR_MESSAGE);
    }
  }
}
