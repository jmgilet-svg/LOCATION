package com.location.client.telemetry;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Simple HUD to display telemetry events collected on the client.
 */
public final class EventLogDialog extends JDialog {
  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm:ss").withLocale(Locale.ROOT);

  private final Metrics metrics = Metrics.get();
  private final JTextArea textArea = new JTextArea();
  private Consumer<Metrics.Event> listener;

  public EventLogDialog(Component parent) {
    super(SwingUtilities.getWindowAncestor(parent), "Journal des événements", false);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout());
    textArea.setEditable(false);
    textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    textArea.setLineWrap(false);
    add(new JScrollPane(textArea), BorderLayout.CENTER);

    JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton clearButton = new JButton("Vider");
    clearButton.addActionListener(
        e -> {
          metrics.clearEvents();
          textArea.setText("(aucun événement)");
        });
    footer.add(clearButton);
    add(footer, BorderLayout.SOUTH);

    populateInitialEvents();
    registerListener();

    setSize(560, 420);
    setLocationRelativeTo(parent);
  }

  private void populateInitialEvents() {
    List<Metrics.Event> events = metrics.recentEvents();
    if (events.isEmpty()) {
      textArea.setText("(aucun événement)");
      return;
    }
    StringBuilder builder = new StringBuilder();
    for (Metrics.Event event : events) {
      if (builder.length() > 0) {
        builder.append(System.lineSeparator());
      }
      builder.append(format(event));
    }
    textArea.setText(builder.toString());
    textArea.setCaretPosition(textArea.getDocument().getLength());
  }

  private void registerListener() {
    listener =
        event -> {
          if (event == null) {
            return;
          }
          SwingUtilities.invokeLater(
              () -> {
                if (textArea.getText().isBlank() || "(aucun événement)".equals(textArea.getText())) {
                  textArea.setText("");
                }
                if (!textArea.getText().isEmpty()) {
                  textArea.append(System.lineSeparator());
                }
                textArea.append(format(event));
                textArea.setCaretPosition(textArea.getDocument().getLength());
              });
        };
    metrics.addEventListener(listener);
    addWindowListener(
        new java.awt.event.WindowAdapter() {
          @Override
          public void windowClosed(java.awt.event.WindowEvent e) {
            cleanup();
          }

          @Override
          public void windowClosing(java.awt.event.WindowEvent e) {
            cleanup();
          }
        });
  }

  private void cleanup() {
    if (listener != null) {
      metrics.removeEventListener(listener);
      listener = null;
    }
  }

  private String format(Metrics.Event event) {
    String timestamp =
        TIME_FORMATTER.format(event.getTimestamp().atZone(ZoneId.systemDefault()));
    StringBuilder builder = new StringBuilder();
    builder.append(timestamp).append(" · ").append(event.getType());
    Map<String, String> attributes = event.getAttributes();
    if (attributes != null && !attributes.isEmpty()) {
      builder.append(" ");
      boolean first = true;
      for (Map.Entry<String, String> entry : attributes.entrySet()) {
        if (!first) {
          builder.append(", ");
        }
        builder.append(entry.getKey()).append("=").append(entry.getValue());
        first = false;
      }
    }
    return builder.toString();
  }
}
