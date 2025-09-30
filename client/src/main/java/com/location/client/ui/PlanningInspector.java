package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import com.location.client.ui.uikit.Chip;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class PlanningInspector extends JPanel {
  private final DataSourceProvider dataSource;
  private final JLabel title = new JLabel("—");
  private final JTextArea notes = new JTextArea(6, 24);
  private final JTextField tags = new JTextField(24);
  private final JPanel chipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
  private final String[] suggestedTags = new String[] {"urgent", "à-confirmer", "zone-nord", "zone-sud", "maintenance", "VIP"};
  private Models.Intervention current;

  public PlanningInspector(DataSourceProvider dataSource) {
    super(new BorderLayout(8, 8));
    this.dataSource = dataSource;
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    setOpaque(false);

    JPanel content = new JPanel();
    content.setOpaque(false);
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

    title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
    content.add(title);
    content.add(Box.createVerticalStrut(8));

    content.add(new JLabel("Notes internes:"));
    notes.setEditable(false);
    notes.setLineWrap(true);
    notes.setWrapStyleWord(true);
    content.add(new JScrollPane(notes));

    content.add(Box.createVerticalStrut(8));
    content.add(new JLabel("Tags (séparés par des virgules):"));
    content.add(tags);
    chipPanel.setOpaque(false);
    content.add(Box.createVerticalStrut(6));
    content.add(chipPanel);
    JButton save = new JButton("Enregistrer les tags");
    save.addActionListener(e -> persistTags());
    content.add(Box.createVerticalStrut(8));
    content.add(save);

    add(content, BorderLayout.NORTH);
    refreshChips(List.of());
  }

  public void showIntervention(Models.Intervention intervention) {
    current = intervention;
    if (intervention == null) {
      title.setText("—");
      notes.setText("");
      tags.setText("");
      refreshChips(List.of());
      return;
    }
    title.setText(intervention.title() == null ? "(Sans titre)" : intervention.title());
    notes.setText(intervention.notes() == null ? "" : intervention.notes());
    try {
      List<String> tagList = dataSource.getInterventionTags(intervention.id());
      tags.setText(String.join(", ", tagList));
      refreshChips(tagList);
    } catch (RuntimeException ex) {
      tags.setText("");
      refreshChips(List.of());
    }
  }

  private void persistTags() {
    if (current == null) {
      return;
    }
    List<String> tagList = parseTagsField();
    try {
      dataSource.setInterventionTags(current.id(), tagList);
    } catch (RuntimeException ex) {
      // Best effort only; errors are silently ignored for now.
    }
    refreshChips(tagList);
  }

  private List<String> parseTagsField() {
    return Arrays.stream(tags.getText().split(","))
        .map(String::trim)
        .filter(token -> !token.isBlank())
        .distinct()
        .collect(Collectors.toList());
  }

  private void refreshChips(List<String> currentTags) {
    chipPanel.removeAll();
    Set<String> selected = new LinkedHashSet<>(currentTags);
    for (String label : suggestedTags) {
      Chip chip = new Chip(label);
      chip.setSelected(selected.contains(label));
      chip.addActionListener(
          e -> {
            if (current == null) {
              return;
            }
            Set<String> combined = new LinkedHashSet<>(parseTagsField());
            if (chip.isSelected()) {
              combined.add(label);
            } else {
              combined.remove(label);
            }
            List<String> combinedList = new ArrayList<>(combined);
            tags.setText(String.join(", ", combinedList));
            try {
              dataSource.setInterventionTags(current.id(), combinedList);
            } catch (RuntimeException ex) {
              // Ignore errors; UI is best-effort.
            }
            refreshChips(combinedList);
          });
      chipPanel.add(chip);
    }
    chipPanel.revalidate();
    chipPanel.repaint();
  }
}
