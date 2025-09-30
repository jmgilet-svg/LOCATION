package com.location.client.ui.uikit;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.Toolkit;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

public final class Toasts {
  private Toasts() {}

  public static void info(Window owner, String message) {
    show(owner, message, new Color(59, 130, 246));
  }

  public static void success(Window owner, String message) {
    show(owner, message, new Color(16, 185, 129));
  }

  public static void error(Window owner, String message) {
    show(owner, message, new Color(239, 68, 68));
  }

  private static void show(Window owner, String message, Color color) {
    JWindow window = new JWindow(owner);
    JLabel label = new JLabel(message);
    label.setForeground(Color.WHITE);
    label.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
    window.add(label);
    window.getRootPane().setBorder(BorderFactory.createLineBorder(color.darker(), 1, true));
    window.getContentPane().setBackground(color);
    window.pack();
    Point location;
    if (owner != null && owner.isShowing()) {
      Point ownerLocation = owner.getLocationOnScreen();
      location =
          new Point(
              ownerLocation.x + owner.getWidth() - window.getWidth() - 24,
              ownerLocation.y + owner.getHeight() - window.getHeight() - 24);
    } else {
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      location = new Point(screen.width - window.getWidth() - 24, screen.height - window.getHeight() - 24);
    }
    window.setLocation(location);
    window.setVisible(true);
    new Timer()
        .schedule(
            new TimerTask() {
              @Override
              public void run() {
                SwingUtilities.invokeLater(window::dispose);
              }
            },
            2200);
  }
}
