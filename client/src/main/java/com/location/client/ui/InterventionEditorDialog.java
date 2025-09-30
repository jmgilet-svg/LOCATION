package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;

/** Dialogue complet pour créer ou modifier une intervention. */
public class InterventionEditorDialog extends JDialog {
  public interface SavedCallback {
    void afterSave(Models.Intervention intervention);
  }

  private final DataSourceProvider dsp;
  private Models.Intervention current;

  private final JTextField titleField = new JTextField(24);
  private final JTextField driverField = new JTextField(20);
  private final JComboBox<Models.Client> clientCombo = new JComboBox<>();
  private final JComboBox<Models.Resource> resourceCombo = new JComboBox<>();
  private final JTextArea notesArea = new JTextArea(4, 24);
  private final JTextArea internalNotesArea = new JTextArea(4, 24);
  private final JSpinner startSpinner = new JSpinner(new SpinnerDateModel());
  private final JSpinner endSpinner = new JSpinner(new SpinnerDateModel());

  private final SavedCallback callback;

  public InterventionEditorDialog(
      Window owner, DataSourceProvider dsp, Models.Intervention base, SavedCallback callback) {
    super(owner, "Intervention", ModalityType.APPLICATION_MODAL);
    this.dsp = dsp;
    this.callback = callback;
    this.current =
        base != null
            ? base
            : new Models.Intervention(
                null,
                null,
                List.of(),
                null,
                null,
                "Nouvelle intervention",
                Instant.now().plus(Duration.ofHours(1)),
                Instant.now().plus(Duration.ofHours(3)),
                null,
                null,
                null);

    setLayout(new BorderLayout());
    getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

    configureSpinners();
    populateCombos();
    fillFields();

    add(buildForm(), BorderLayout.CENTER);
    add(buildButtons(), BorderLayout.SOUTH);

    pack();
    setMinimumSize(getSize());
    setLocationRelativeTo(owner);
  }

  private void configureSpinners() {
    startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "yyyy-MM-dd HH:mm"));
    endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "yyyy-MM-dd HH:mm"));
  }

  private JPanel buildForm() {
    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.insets = new Insets(4, 6, 4, 6);
    c.anchor = GridBagConstraints.WEST;

    addLabel(form, c, "Titre");
    addField(form, c, titleField);
    addLabel(form, c, "Ressource");
    addField(form, c, resourceCombo);
    addLabel(form, c, "Client");
    addField(form, c, clientCombo);
    addLabel(form, c, "Chauffeur (id)");
    addField(form, c, driverField);
    addLabel(form, c, "Début");
    addField(form, c, startSpinner);
    addLabel(form, c, "Fin");
    addField(form, c, endSpinner);

    c.gridx = 0;
    c.gridy++;
    c.gridwidth = 2;
    c.fill = GridBagConstraints.BOTH;
    notesArea.setLineWrap(true);
    notesArea.setWrapStyleWord(true);
    form.add(new JLabel("Notes"), c);
    c.gridy++;
    form.add(new JScrollPane(notesArea), c);

    c.gridy++;
    internalNotesArea.setLineWrap(true);
    internalNotesArea.setWrapStyleWord(true);
    form.add(new JLabel("Notes internes"), c);
    c.gridy++;
    form.add(new JScrollPane(internalNotesArea), c);

    return form;
  }

  private JPanel buildButtons() {
    JPanel buttons = new JPanel();
    JButton quoteButton =
        new JButton(
            new AbstractAction("Créer/Modifier le devis") {
              @Override
              public void actionPerformed(ActionEvent e) {
                Models.Client client = (Models.Client) clientCombo.getSelectedItem();
                Models.Resource resource = (Models.Resource) resourceCombo.getSelectedItem();
                if (client == null || resource == null) {
                  notifyError("Sélectionne client et ressource");
                  return;
                }
                String driver = driverField.getText().trim();
                if (driver.isBlank()) {
                  driver = null;
                }
                String title = titleField.getText().trim();
                if (title.isBlank()) {
                  notifyError("Titre requis");
                  return;
                }
                Date sd = (Date) startSpinner.getValue();
                Date ed = (Date) endSpinner.getValue();
                Instant start = sd.toInstant();
                Instant end = ed.toInstant();
                String notes = notesArea.getText().trim();
                if (notes.isBlank()) {
                  notes = null;
                }
                String internalNotes = internalNotesArea.getText().trim();
                if (internalNotes.isBlank()) {
                  internalNotes = null;
                }
                boolean isCreation = current.id() == null;
                Models.Intervention payload =
                    new Models.Intervention(
                        current.id(),
                        resource.agencyId(),
                        resource.id(),
                        client.id(),
                        driver,
                        title,
                        start,
                        end,
                        notes,
                        internalNotes);
                Models.Intervention effective = payload;
                if (isCreation) {
                  effective = dsp.createIntervention(payload);
                }
                current = effective;
                dsp.createQuoteFromIntervention(effective);
                notifySuccess("Devis créé/mis à jour");
              }
            });
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
    buttons.add(quoteButton);
    buttons.add(cancel);
    buttons.add(save);
    return buttons;
  }

  private void addLabel(JPanel panel, GridBagConstraints c, String text) {
    c.gridx = 0;
    c.fill = GridBagConstraints.NONE;
    panel.add(new JLabel(text), c);
  }

  private void addField(JPanel panel, GridBagConstraints c, java.awt.Component component) {
    c.gridx = 1;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    panel.add(component, c);
    c.gridy++;
    c.weightx = 0;
    c.fill = GridBagConstraints.NONE;
  }

  private void populateCombos() {
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
    clientCombo.setRenderer(
        new DefaultListCellRenderer() {
          @Override
          public java.awt.Component getListCellRendererComponent(
              javax.swing.JList<?> list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Models.Client client) {
              setText(client.name());
            }
            return this;
          }
        });

    resourceCombo.removeAllItems();
    List<Models.Resource> resources = dsp.listResources();
    for (Models.Resource resource : resources) {
      resourceCombo.addItem(resource);
    }

    clientCombo.removeAllItems();
    List<Models.Client> clients = dsp.listClients();
    for (Models.Client client : clients) {
      clientCombo.addItem(client);
    }
  }

  private void fillFields() {
    titleField.setText(current.title() != null ? current.title() : "");
    driverField.setText(current.driverId() != null ? current.driverId() : "");
    notesArea.setText(current.notes() != null ? current.notes() : "");
    internalNotesArea.setText(current.internalNotes() != null ? current.internalNotes() : "");

    if (current.start() != null) {
      startSpinner.setValue(Date.from(current.start()));
    }
    if (current.end() != null) {
      endSpinner.setValue(Date.from(current.end()));
    }

    selectComboItem(resourceCombo, current.resourceId(), Models.Resource::id);
    selectComboItem(clientCombo, current.clientId(), Models.Client::id);
  }

  private <T> void selectComboItem(
      JComboBox<T> combo, String id, java.util.function.Function<T, String> idExtractor) {
    if (id == null) {
      return;
    }
    for (int i = 0; i < combo.getItemCount(); i++) {
      T element = combo.getItemAt(i);
      if (element != null && id.equals(idExtractor.apply(element))) {
        combo.setSelectedIndex(i);
        break;
      }
    }
  }

  private void save() {
    try {
      String title = titleField.getText().trim();
      if (title.isBlank()) {
        throw new IllegalArgumentException("Le titre ne peut pas être vide");
      }
      Models.Resource resource = (Models.Resource) resourceCombo.getSelectedItem();
      if (resource == null) {
        throw new IllegalArgumentException("Sélectionnez une ressource");
      }
      Models.Client client = (Models.Client) clientCombo.getSelectedItem();
      if (client == null) {
        throw new IllegalArgumentException("Sélectionnez un client");
      }
      Date startDate = (Date) startSpinner.getValue();
      Date endDate = (Date) endSpinner.getValue();
      if (!endDate.after(startDate)) {
        throw new IllegalArgumentException("La fin doit être après le début");
      }
      Instant start = startDate.toInstant();
      Instant end = endDate.toInstant();
      ensureAvailability(resource.id(), start, end);

      String driver = driverField.getText().trim();
      if (driver.isBlank()) {
        driver = null;
      }
      String notes = notesArea.getText();
      if (notes != null && notes.isBlank()) {
        notes = null;
      }
      String internalNotes = internalNotesArea.getText();
      if (internalNotes != null && internalNotes.isBlank()) {
        internalNotes = null;
      }

      boolean isCreation = current.id() == null;
      Models.Intervention payload =
          new Models.Intervention(
              current.id(),
              resource.agencyId(),
              resource.id(),
              client.id(),
              driver,
              title,
              start,
              end,
              notes,
              internalNotes);

      Models.Intervention saved =
          isCreation ? dsp.createIntervention(payload) : dsp.updateIntervention(payload);
      notifySuccess(isCreation ? "Intervention créée" : "Intervention mise à jour");
      ActivityCenter.log((isCreation ? "Création" : "Mise à jour") + " intervention " + saved.id());
      current = saved;
      if (callback != null) {
        callback.afterSave(saved);
      }
      dispose();
    } catch (RuntimeException ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void ensureAvailability(String resourceId, Instant start, Instant end) {
    if (resourceId == null || start == null || end == null) {
      return;
    }
    try {
      List<Models.Unavailability> unavailabilities =
          dsp.listUnavailabilities(
              OffsetDateTime.ofInstant(start, ZoneOffset.UTC),
              OffsetDateTime.ofInstant(end, ZoneOffset.UTC),
              resourceId);
      for (Models.Unavailability unavailability : unavailabilities) {
        if (!resourceId.equals(unavailability.resourceId())) {
          continue;
        }
        if (unavailability.end().isAfter(start) && unavailability.start().isBefore(end)) {
          String reason = unavailability.reason();
          String detail = (reason == null || reason.isBlank()) ? "" : " : " + reason;
          throw new IllegalStateException("Chevauche une indisponibilité" + detail);
        }
      }
    } catch (RuntimeException ex) {
      String message = ex.getMessage();
      if (message != null && message.toLowerCase(Locale.ROOT).contains("non disponible")) {
        return;
      }
      throw ex;
    }
  }

  private void notifySuccess(String message) {
    Window owner = getOwner();
    if (owner instanceof MainFrame mf) {
      mf.toastSuccess(message);
    } else if (owner != null) {
      Toast.success(owner, message);
    }
  }
}
