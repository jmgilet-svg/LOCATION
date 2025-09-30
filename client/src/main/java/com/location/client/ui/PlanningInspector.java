package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import com.location.client.ui.uikit.Chip;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

public class PlanningInspector extends JPanel {
  private final DataSourceProvider dataSource;
  private final JLabel title = new JLabel("—");
  private final JTextArea notes = new JTextArea(6, 24);
  private final JTextField tags = new JTextField(24);
  private final JPanel chipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
  private final JPopupMenu autocompleteMenu = new JPopupMenu();
  private final DefaultListModel<String> autocompleteModel = new DefaultListModel<>();
  private final JList<String> autocompleteList = new JList<>(autocompleteModel);
  private String[] suggestions =
      new String[] {"urgent", "à-confirmer", "zone-nord", "zone-sud", "maintenance", "VIP"};
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

    setupAutocomplete();
    loadSuggestions();
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
    for (String label : suggestions) {
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

  private void loadSuggestions() {
    CompletableFuture
        .supplyAsync(
            () -> {
              try {
                List<String> remote = dataSource.suggestTags(24);
                if (remote == null || remote.isEmpty()) {
                  return null;
                }
                return new ArrayList<>(new LinkedHashSet<>(remote));
              } catch (RuntimeException ex) {
                return null;
              }
            })
        .thenAccept(
            remote -> {
              if (remote == null || remote.isEmpty()) {
                return;
              }
              SwingUtilities.invokeLater(
                  () -> {
                    suggestions = remote.toArray(new String[0]);
                    refreshChips(parseTagsField());
                  });
            });
  }

  private void setupAutocomplete() {
    autocompleteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    autocompleteList.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
              acceptAutocomplete();
            }
          }
        });
    JScrollPane scroll = new JScrollPane(autocompleteList);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    autocompleteMenu.setFocusable(false);
    autocompleteMenu.add(scroll);

    tags.getDocument()
        .addDocumentListener(
            new DocumentListener() {
              private void refresh() {
                showAutocomplete();
              }

              @Override
              public void insertUpdate(DocumentEvent e) {
                refresh();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                refresh();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                refresh();
              }
            });

    tags.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (!autocompleteMenu.isVisible()) {
              return;
            }
            switch (e.getKeyCode()) {
              case KeyEvent.VK_DOWN -> {
                int next = Math.min(autocompleteList.getSelectedIndex() + 1, autocompleteModel.size() - 1);
                autocompleteList.setSelectedIndex(next);
                autocompleteList.ensureIndexIsVisible(next);
                e.consume();
              }
              case KeyEvent.VK_UP -> {
                int prev = Math.max(autocompleteList.getSelectedIndex() - 1, 0);
                autocompleteList.setSelectedIndex(prev);
                autocompleteList.ensureIndexIsVisible(prev);
                e.consume();
              }
              case KeyEvent.VK_ENTER -> {
                acceptAutocomplete();
                e.consume();
              }
              case KeyEvent.VK_ESCAPE -> {
                autocompleteMenu.setVisible(false);
                e.consume();
              }
              default -> {}
            }
          }
        });

    tags.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusLost(FocusEvent e) {
            autocompleteMenu.setVisible(false);
          }
        });
  }

  private void showAutocomplete() {
    String text = tags.getText();
    int caret = tags.getCaretPosition();
    int lastComma = text.lastIndexOf(',', Math.max(0, caret - 1));
    String fragment = text.substring(lastComma + 1, caret).trim().toLowerCase();
    autocompleteModel.clear();

    if (fragment.isEmpty()) {
      autocompleteMenu.setVisible(false);
      return;
    }

    Set<String> existing = new LinkedHashSet<>(parseTagsField());
    for (String suggestion : suggestions) {
      if (suggestion.toLowerCase().contains(fragment) && !existing.contains(suggestion)) {
        autocompleteModel.addElement(suggestion);
      }
    }

    if (autocompleteModel.isEmpty()) {
      autocompleteMenu.setVisible(false);
      return;
    }

    autocompleteList.setSelectedIndex(0);
    autocompleteList.ensureIndexIsVisible(0);

    try {
      java.awt.Rectangle caretBounds = tags.modelToView(caret);
      if (caretBounds != null) {
        autocompleteMenu.setPopupSize(Math.max(180, tags.getWidth()), 140);
        autocompleteMenu.show(tags, caretBounds.x, caretBounds.y + caretBounds.height);
        return;
      }
    } catch (BadLocationException ignored) {
      // fallback below
    }

    autocompleteMenu.setPopupSize(Math.max(180, tags.getWidth()), 140);
    autocompleteMenu.show(tags, 0, tags.getHeight());
  }

  private void acceptAutocomplete() {
    String choice = autocompleteList.getSelectedValue();
    if (choice == null || choice.isBlank()) {
      return;
    }
    Set<String> combined = new LinkedHashSet<>(parseTagsField());
    combined.add(choice);
    List<String> combinedList = new ArrayList<>(combined);
    tags.setText(String.join(", ", combinedList));
    refreshChips(combinedList);
    autocompleteMenu.setVisible(false);
  }
}
