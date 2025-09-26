package com.location.client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CommandPaletteDialog extends JDialog {
  public record Command(String id, String label, Runnable action) {}

  private final JTextField input = new JTextField();
  private final DefaultListModel<Command> model = new DefaultListModel<>();
  private final JList<Command> list = new JList<>(model);
  private final List<Command> all = new ArrayList<>();

  public CommandPaletteDialog(Window owner) {
    super(owner, "Commande rapide", ModalityType.APPLICATION_MODAL);
    setLayout(new BorderLayout(6, 6));
    input.getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                filter();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                filter();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                filter();
              }
            });
    add(input, BorderLayout.NORTH);

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(
        new DefaultListCellRenderer() {
          @Override
          public java.awt.Component getListCellRendererComponent(
              JList<?> jList, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            var comp =
                (java.awt.Component)
                    super.getListCellRendererComponent(jList, value, index, isSelected, cellHasFocus);
            if (value instanceof Command command && comp instanceof javax.swing.JLabel label) {
              label.setText(command.label());
              label.setIcon(IconLoader.lightning());
            }
            return comp;
          }
        });
    add(new JScrollPane(list), BorderLayout.CENTER);

    JButton runButton = new JButton(new AbstractAction("Exécuter") {
      @Override
      public void actionPerformed(ActionEvent e) {
        execute();
      }
    });
    getRootPane().setDefaultButton(runButton);
    add(runButton, BorderLayout.SOUTH);

    input.addActionListener(e -> execute());
    list.addListSelectionListener(e -> updateDefaultSelection());

    setPreferredSize(new Dimension(520, 380));
    pack();
    setLocationRelativeTo(owner);
  }

  private void updateDefaultSelection() {
    if (list.getSelectedIndex() < 0 && !model.isEmpty()) {
      list.setSelectedIndex(0);
    }
  }

  public CommandPaletteDialog commands(Command... commands) {
    all.clear();
    model.clear();
    for (Command command : commands) {
      all.add(command);
      model.addElement(command);
    }
    if (!model.isEmpty()) {
      list.setSelectedIndex(0);
    }
    return this;
  }

  private void filter() {
    String query = input.getText() == null ? "" : input.getText().trim().toLowerCase();
    model.clear();
    for (Command command : all) {
      if (matches(command.label(), query)) {
        model.addElement(command);
      }
    }
    updateDefaultSelection();
  }

  private boolean matches(String label, String query) {
    if (query.isBlank()) {
      return true;
    }
    String haystack = label.toLowerCase();
    int pos = 0;
    for (char ch : query.toCharArray()) {
      pos = haystack.indexOf(ch, pos);
      if (pos < 0) {
        return false;
      }
      pos++;
    }
    return true;
  }

  private void execute() {
    Command selected = list.getSelectedValue();
    if (selected == null && !model.isEmpty()) {
      selected = model.get(0);
    }
    if (selected != null) {
      Runnable action = selected.action();
      if (action != null) {
        SwingUtilities.invokeLater(action);
      }
      setVisible(false);
      dispose();
    }
  }
}
