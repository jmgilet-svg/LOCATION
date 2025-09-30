package com.location.client.ui.uikit;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class FormField extends JPanel {
  private final JLabel label = new JLabel();
  private final JComponent input;
  private final JLabel hint = new JLabel();
  private final JLabel error = new JLabel();

  public FormField(String labelText, JComponent input) {
    super(new BorderLayout(4, 4));
    this.input = input;
    setOpaque(false);
    label.setText(labelText);
    label.setLabelFor(input);
    hint.setForeground(new java.awt.Color(100, 100, 120));
    error.setForeground(new java.awt.Color(200, 0, 0));
    error.setVisible(false);

    add(label, BorderLayout.NORTH);
    add(input, BorderLayout.CENTER);

    JPanel south = new JPanel(new BorderLayout());
    south.setOpaque(false);
    south.add(hint, BorderLayout.WEST);
    south.add(error, BorderLayout.EAST);
    south.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    add(south, BorderLayout.SOUTH);
  }

  public void setHint(String hintText) {
    hint.setText(hintText == null ? "" : hintText);
  }

  public void setError(String errorText) {
    error.setText(errorText == null ? "" : errorText);
    error.setVisible(errorText != null && !errorText.isBlank());
  }

  public JComponent getInput() {
    return input;
  }
}
