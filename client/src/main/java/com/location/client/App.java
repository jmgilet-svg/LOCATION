package com.location.client;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Preferences;
import com.location.client.ui.MainFrame;
import com.location.client.ui.Theme;
import javax.swing.SwingUtilities;
import java.awt.EventQueue;

public class App {
  public static void main(String[] args) {
    EventQueue.invokeLater(() -> {
      Theme.ensureInitialized();
      new StartupSelector(args, App::launch).showAndRun();
    });
  }

  private static void launch(DataSourceProvider dsp, Preferences prefs) {
    SwingUtilities.invokeLater(() -> {
      MainFrame frame = new MainFrame(dsp, prefs);
      frame.setVisible(true);
    });
  }
}
