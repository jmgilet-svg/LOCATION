package com.location.client.ui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;

public final class ActivityCenter {
  private static final DefaultListModel<String> MODEL = new DefaultListModel<>();
  private static final int MAX_EVENTS = 250;
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

  private ActivityCenter() {}

  public static synchronized void log(String message) {
    if (MODEL.getSize() >= MAX_EVENTS) {
      MODEL.removeElementAt(MODEL.getSize() - 1);
    }
    MODEL.add(0, OffsetDateTime.now().format(FORMATTER) + " — " + message);
  }

  public static JDialog dialog(Window owner) {
    JDialog dialog = new JDialog(owner, "Activité récente", Dialog.ModalityType.MODELESS);
    JList<String> list = new JList<>(MODEL);
    dialog.setLayout(new BorderLayout());
    dialog.add(new JScrollPane(list), BorderLayout.CENTER);
    dialog.setSize(520, 360);
    dialog.setLocationRelativeTo(owner);
    return dialog;
  }
}
