package com.location.client.ui.accordion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CollapsibleSection extends JPanel {
  private final JPanel header = new JPanel(new BorderLayout());
  private final JButton toggle = new JButton();
  private final JLabel title = new JLabel();
  private final JPanel content = new JPanel(new BorderLayout());
  private boolean expanded;

  public CollapsibleSection(String text) {
    super(new BorderLayout());
    toggle.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
    toggle.setContentAreaFilled(false);
    toggle.setFocusPainted(false);
    header.add(toggle, BorderLayout.WEST);
    header.add(title, BorderLayout.CENTER);
    header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
    add(header, BorderLayout.NORTH);
    add(content, BorderLayout.CENTER);
    setExpanded(false);
    setTitle(text);
    toggle.addActionListener(e -> setExpanded(!expanded));
  }

  public void setTitle(String text) {
    title.setText(text);
  }

  public void setContent(Component component) {
    content.removeAll();
    if (component != null) {
      content.add(component, BorderLayout.CENTER);
    }
    content.revalidate();
    content.repaint();
  }

  public void setExpanded(boolean on) {
    expanded = on;
    toggle.setText(on ? "▾" : "▸");
    content.setVisible(on);
    revalidate();
  }
}
