package com.location.client.ui.uikit;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Super simple app-wide notification bus (thread-safe). */
public final class Notify {
  private static final PropertyChangeSupport SUPPORT = new PropertyChangeSupport(Notify.class);

  private Notify() {}

  public static void on(String event, PropertyChangeListener listener) {
    SUPPORT.addPropertyChangeListener(event, listener);
  }

  public static void off(String event, PropertyChangeListener listener) {
    SUPPORT.removePropertyChangeListener(event, listener);
  }

  public static void post(String event, Object payload) {
    SUPPORT.firePropertyChange(event, null, payload);
  }
}
