package com.location.client.ui.uikit;

import javax.swing.SwingUtilities;

/** Small helpers to enforce Swing EDT discipline. */
public final class Ui {
  private Ui() {}

  /** Run later on the EDT. Safe to call from any thread. */
  public static void later(Runnable runnable) {
    if (runnable == null) {
      return;
    }
    SwingUtilities.invokeLater(runnable);
  }

  /** Run immediately if already on the EDT, otherwise schedule later. */
  public static void ensure(Runnable runnable) {
    if (runnable == null) {
      return;
    }
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
    }
  }
}
