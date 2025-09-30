package com.location.client.ui;

import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/** Simple merge UI for two clients, writes back into the table model. */
public class ClientsMergeDialog extends JDialog {
  private final ButtonGroup nameGroup = new ButtonGroup();
  private final JRadioButton nameA = new JRadioButton();
  private final JRadioButton nameB = new JRadioButton();
  private final ButtonGroup emailGroup = new ButtonGroup();
  private final JRadioButton emailA = new JRadioButton();
  private final JRadioButton emailB = new JRadioButton();
  private boolean ok;

  public ClientsMergeDialog(JFrame owner, Models.Client a, Models.Client b) {
    super(owner, "Fusionner deux clients", true);
    setLayout(new BorderLayout(8, 8));

    JPanel form = new JPanel(new GridLayout(0, 3, 8, 8));
    form.add(new JLabel("Champ"));
    form.add(new JLabel("Client A"));
    form.add(new JLabel("Client B"));

    form.add(new JLabel("Nom"));
    nameA.setText(valueOrEmpty(a.name()));
    nameB.setText(valueOrEmpty(b.name()));
    nameGroup.add(nameA);
    nameGroup.add(nameB);
    nameA.setSelected(true);
    form.add(nameA);
    form.add(nameB);

    form.add(new JLabel("Email"));
    emailA.setText(valueOrEmpty(a.email()));
    emailB.setText(valueOrEmpty(b.email()));
    emailGroup.add(emailA);
    emailGroup.add(emailB);
    emailA.setSelected(true);
    form.add(emailA);
    form.add(emailB);

    add(form, BorderLayout.CENTER);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton cancel = new JButton("Annuler");
    JButton merge = new JButton("Fusionner");
    cancel.addActionListener(e -> {
      ok = false;
      setVisible(false);
    });
    merge.addActionListener(e -> {
      ok = true;
      setVisible(false);
    });
    actions.add(cancel);
    actions.add(merge);
    add(actions, BorderLayout.SOUTH);

    pack();
    setLocationRelativeTo(owner);
  }

  public boolean isOk() {
    return ok;
  }

  public Models.Client merged(Models.Client a, Models.Client b) {
    String name = nameB.isSelected() ? nameB.getText() : nameA.getText();
    String email = emailB.isSelected() ? emailB.getText() : emailA.getText();
    return new Models.Client(
        a.id(),
        nullIfBlank(name),
        nullIfBlank(email),
        a.phone(),
        a.address(),
        a.zip(),
        a.city(),
        a.vatNumber(),
        a.iban());
  }

  private static String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String nullIfBlank(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
