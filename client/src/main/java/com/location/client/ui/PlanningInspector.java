package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;
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
    JButton save = new JButton("Enregistrer les tags");
    save.addActionListener(e -> persistTags());
    content.add(Box.createVerticalStrut(8));
    content.add(save);

    add(content, BorderLayout.NORTH);
  }

  public void showIntervention(Models.Intervention intervention) {
    current = intervention;
    if (intervention == null) {
      title.setText("—");
      notes.setText("");
      tags.setText("");
      return;
    }
    title.setText(intervention.title() == null ? "(Sans titre)" : intervention.title());
    notes.setText(intervention.notes() == null ? "" : intervention.notes());
    try {
      List<String> tagList = dataSource.getInterventionTags(intervention.id());
      tags.setText(String.join(", ", tagList));
    } catch (RuntimeException ex) {
      tags.setText("");
    }
  }

  private void persistTags() {
    if (current == null) {
      return;
    }
    List<String> tagList =
        Arrays.stream(tags.getText().split(","))
            .map(String::trim)
            .filter(token -> !token.isBlank())
            .collect(Collectors.toList());
    try {
      dataSource.setInterventionTags(current.id(), tagList);
    } catch (RuntimeException ex) {
      // Best effort only; errors are silently ignored for now.
    }
  }
}
