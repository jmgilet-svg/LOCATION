package com.location.client;

import com.location.client.core.Cli;
import com.location.client.core.DataSourceProvider;
import com.location.client.core.MockDataSource;
import com.location.client.core.Preferences;
import com.location.client.core.RestDataSource;
import com.location.client.ui.Theme;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.function.BiConsumer;

public class StartupSelector {
  private final String[] args;
  private final BiConsumer<DataSourceProvider, Preferences> onReady;

  public StartupSelector(String[] args, BiConsumer<DataSourceProvider, Preferences> onReady) {
    this.args = args;
    this.onReady = onReady;
  }

  public void showAndRun() {
    Preferences prefs = Preferences.load();
    Theme.initialize(prefs);
    String forced = Cli.parseDataSourceArg(args);
    if (forced != null) {
      DataSourceProvider dsp = forced.equals("rest")
          ? new RestDataSource(prefs.getBaseUrl(), prefs.getRestUser(), prefs.getRestPass())
          : new MockDataSource();
      onReady.accept(dsp, prefs);
      return;
    }
    String remembered = prefs.getLastSource();
    if (remembered != null) {
      DataSourceProvider dsp = remembered.equals("rest")
          ? new RestDataSource(prefs.getBaseUrl(), prefs.getRestUser(), prefs.getRestPass())
          : new MockDataSource();
      onReady.accept(dsp, prefs);
      return;
    }

    JCheckBox remember = new JCheckBox("Mémoriser ce choix");
    JButton mock = new JButton("Mode Démo (Mock)");
    JButton rest = new JButton("Mode Connecté (Backend)");
    JLabel info = new JLabel("Sélection de la source de données");
    JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
    panel.add(info);
    panel.add(mock);
    panel.add(rest);
    panel.add(remember);
    JDialog dialog = new JDialog((Frame) null, "LOCATION — Sélection", true);
    dialog.getContentPane().add(panel);
    dialog.pack();
    dialog.setLocationRelativeTo(null);

    mock.addActionListener(e -> {
      if (remember.isSelected()) {
        prefs.setLastSource("mock");
        prefs.save();
      }
      onReady.accept(new MockDataSource(), prefs);
      dialog.dispose();
    });
    rest.addActionListener(e -> {
      if (remember.isSelected()) {
        prefs.setLastSource("rest");
        prefs.save();
      }
      onReady.accept(new RestDataSource(prefs.getBaseUrl(), prefs.getRestUser(), prefs.getRestPass()), prefs);
      dialog.dispose();
    });
    dialog.setVisible(true);
  }
}
