package com.location.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;

/** Toast léger affiché en bas droite, pile d'attente simple. */
public final class Toast {
  private static final Deque<JWindow> queue = new ArrayDeque<>();

  private Toast() {}

  public static void info(Window owner, String message) {
    show(owner, message, new Color(33, 150, 243));
  }

  public static void success(Window owner, String message) {
    show(owner, message, new Color(46, 125, 50));
  }

  public static void error(Window owner, String message) {
    show(owner, message, new Color(198, 40, 40));
  }

  private static synchronized void show(Window owner, String message, Color background) {
    JWindow window = new JWindow(owner);
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    panel.setBackground(background);
    JLabel label = new JLabel("<html><b>" + message + "</b></html>");
    label.setForeground(Color.WHITE);
    panel.add(label);
    window.add(panel);
    window.pack();

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screen.width - window.getWidth() - 24;
    int y = screen.height - window.getHeight() - 60 - queue.size() * (window.getHeight() + 10);
    window.setLocation(x, y);
    window.setAlwaysOnTop(true);
    window.setFocusableWindowState(false);
    window.setVisible(true);
    queue.addLast(window);

    Timer timer = new Timer(3200, e -> dismiss(window));
    timer.setRepeats(false);
    timer.start();
  }

  private static synchronized void dismiss(JWindow window) {
    if (!queue.remove(window)) {
      return;
    }
    window.setVisible(false);
    window.dispose();
    int idx = 0;
    for (JWindow w : queue) {
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screen.width - w.getWidth() - 24;
      int y = screen.height - w.getHeight() - 60 - idx * (w.getHeight() + 10);
      w.setLocation(x, y);
      idx++;
    }
  }
}
