package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Panneau latéral listant des pistes de résolution rapide pour la sélection courante.
 */
public class SuggestionPanel extends JPanel {
  private final DataSourceProvider dsp;
  private final DefaultListModel<String> model = new DefaultListModel<>();
  private final JList<String> suggestions = new JList<>(model);
  private final ZoneId zone = ZoneId.systemDefault();
  private List<Models.Intervention> current = List.of();
  private List<Models.Intervention> dayItems = List.of();
  private Runnable afterApply;
  private int startHour = 7;
  private int endHour = 19;

  public SuggestionPanel(DataSourceProvider dsp) {
    super(new BorderLayout(6, 6));
    this.dsp = dsp;
    setBorder(BorderFactory.createTitledBorder("Suggestions"));
    setPreferredSize(new Dimension(260, 320));
    add(new JScrollPane(suggestions), BorderLayout.CENTER);

    JPanel actions = new JPanel(new GridLayout(0, 1, 6, 6));
    actions.add(new JButton(new ShiftAction("Décaler +30 min", Duration.ofMinutes(30))));
    actions.add(new JButton(new ShiftAction("Décaler −30 min", Duration.ofMinutes(-30))));
    actions.add(new JButton(new SwitchResourceAction()));
    add(actions, BorderLayout.SOUTH);

    model.addElement("Aucune sélection.");
  }

  public void setHours(int startHour, int endHour) {
    this.startHour = startHour;
    this.endHour = endHour;
  }

  public void setAfterApply(Runnable afterApply) {
    this.afterApply = afterApply;
  }

  public void showFor(List<Models.Intervention> selection, List<Models.Intervention> dayItems) {
    this.current = selection == null ? List.of() : List.copyOf(selection);
    this.dayItems = dayItems == null ? List.of() : List.copyOf(dayItems);
    model.clear();
    if (this.current.isEmpty()) {
      model.addElement("Aucune sélection.");
      return;
    }
    Models.Intervention ref = this.current.get(0);
    LocalDate day = ref.start().atZone(zone).toLocalDate();
    model.addElement("Sélection : " + (ref.title() != null ? ref.title() : ref.id()));
    model.addElement("—");
    model.addElement("Créneaux libres (ressource actuelle) :");
    List<String> slots = computeFreeSlots(ref.resourceId(), day);
    if (slots.isEmpty()) {
      model.addElement("  Aucun créneau évident");
    } else {
      for (String slot : slots) {
        model.addElement("  • " + slot);
      }
    }
  }

  private List<String> computeFreeSlots(String resourceId, LocalDate day) {
    List<String> slots = new ArrayList<>();
    int steps = Math.max(0, (endHour - startHour));
    for (int h = 0; h < steps; h++) {
      Instant slotStart = day.atTime(startHour + h, 0).atZone(zone).toInstant();
      Instant slotEnd = slotStart.plus(Duration.ofHours(1));
      boolean busy = dayItems.stream()
          .filter(it -> Objects.equals(it.resourceId(), resourceId))
          .anyMatch(it -> overlaps(slotStart, slotEnd, it.start(), it.end()));
      if (!busy) {
        slots.add(String.format("%02d:00–%02d:00", startHour + h, startHour + h + 1));
      }
    }
    return slots;
  }

  private boolean overlaps(Instant s1, Instant e1, Instant s2, Instant e2) {
    return s1.isBefore(e2) && s2.isBefore(e1);
  }

  private final class ShiftAction extends AbstractAction {
    private final Duration delta;

    private ShiftAction(String name, Duration delta) {
      super(name);
      this.delta = delta;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (current.isEmpty()) {
        return;
      }
      List<Models.Intervention> originals = new ArrayList<>(current);
      try {
        for (Models.Intervention it : current) {
          Models.Intervention updated =
              new Models.Intervention(
                  it.id(),
                  it.agencyId(),
                  it.resourceId(),
                  it.clientId(),
                  it.driverId(),
                  it.title(),
                  it.start().plus(delta),
                  it.end().plus(delta),
                  it.notes());
          dsp.updateIntervention(updated);
        }
        notifySuccess("Déplacement appliqué.");
      } catch (RuntimeException ex) {
        for (Models.Intervention it : originals) {
          try {
            dsp.updateIntervention(it);
          } catch (RuntimeException ignored) {
          }
        }
        JOptionPane.showMessageDialog(
            SuggestionPanel.this,
            "Opération annulée : " + ex.getMessage(),
            "Erreur",
            JOptionPane.WARNING_MESSAGE);
        refresh();
      }
    }
  }

  private final class SwitchResourceAction extends AbstractAction {
    private SwitchResourceAction() {
      super("Changer de ressource");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (current.isEmpty()) {
        return;
      }
      Models.Intervention ref = current.get(0);
      List<Models.Resource> resources = dsp.listResources();
      Models.Resource target =
          resources.stream()
              .filter(r -> !Objects.equals(r.id(), ref.resourceId()))
              .filter(r -> isFree(r.id(), ref.start(), ref.end()))
              .findFirst()
              .orElse(null);
      if (target == null) {
        JOptionPane.showMessageDialog(
            SuggestionPanel.this,
            "Pas d'autre ressource libre détectée.",
            "Information",
            JOptionPane.INFORMATION_MESSAGE);
        return;
      }
      List<Models.Intervention> originals = new ArrayList<>(current);
      try {
        for (Models.Intervention it : current) {
          Models.Intervention updated =
              new Models.Intervention(
                  it.id(),
                  target.agencyId(),
                  target.id(),
                  it.clientId(),
                  it.driverId(),
                  it.title(),
                  it.start(),
                  it.end(),
                  it.notes());
          dsp.updateIntervention(updated);
        }
        notifySuccess("Ressource remplacée.");
      } catch (RuntimeException ex) {
        for (Models.Intervention it : originals) {
          try {
            dsp.updateIntervention(it);
          } catch (RuntimeException ignored) {
          }
        }
        JOptionPane.showMessageDialog(
            SuggestionPanel.this,
            "Opération annulée : " + ex.getMessage(),
            "Erreur",
            JOptionPane.WARNING_MESSAGE);
        refresh();
      }
    }

    private boolean isFree(String resourceId, Instant start, Instant end) {
      return dayItems.stream()
          .filter(it -> Objects.equals(it.resourceId(), resourceId))
          .noneMatch(it -> overlaps(start, end, it.start(), it.end()));
    }
  }

  private void notifySuccess(String message) {
    refresh();
    JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
  }

  private void refresh() {
    if (afterApply != null) {
      afterApply.run();
    }
  }
}
