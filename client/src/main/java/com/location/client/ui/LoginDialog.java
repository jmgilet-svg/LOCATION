package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.RestDataSource;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginDialog extends JDialog {
  private final JTextField userField;
  private final JPasswordField passField;
  private boolean ok;

  public LoginDialog(JFrame owner) {
    super(owner, "Connexion", true);
    userField = new JTextField(System.getenv().getOrDefault("LOCATION_USERNAME", "demo"));
    passField = new JPasswordField(System.getenv().getOrDefault("LOCATION_PASSWORD", "demo"));
    buildUi(owner);
  }

  private void buildUi(JFrame owner) {
    setLayout(new BorderLayout(8, 8));
    JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
    form.add(new JLabel("Utilisateur"));
    form.add(userField);
    form.add(new JLabel("Mot de passe"));
    form.add(passField);
    add(form, BorderLayout.CENTER);
    JPanel buttons = new JPanel();
    JButton okButton =
        new JButton(
            new AbstractAction("OK") {
              @Override
              public void actionPerformed(ActionEvent e) {
                ok = true;
                dispose();
              }
            });
    JButton cancelButton =
        new JButton(
            new AbstractAction("Annuler") {
              @Override
              public void actionPerformed(ActionEvent e) {
                ok = false;
                dispose();
              }
            });
    buttons.add(okButton);
    buttons.add(cancelButton);
    add(buttons, BorderLayout.SOUTH);
    setPreferredSize(new Dimension(360, 160));
    pack();
    setLocationRelativeTo(owner);
  }

  public boolean showDialog() {
    setVisible(true);
    return ok;
  }

  public String getUsername() {
    return userField.getText();
  }

  public String getPassword() {
    return new String(passField.getPassword());
  }

  public static void open(JFrame owner, DataSourceProvider provider) {
    if (!(provider instanceof RestDataSource rd)) {
      JOptionPane.showMessageDialog(
          owner,
          "Connexion configurable uniquement pour la source REST.",
          "Connexion",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    LoginDialog dialog = new LoginDialog(owner);
    if (dialog.showDialog()) {
      rd.setCredentials(dialog.getUsername(), dialog.getPassword());
      JOptionPane.showMessageDialog(
          owner,
          "Identifiants enregistrés. La prochaine requête effectuera la connexion.",
          "Connexion",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }
}
