package com.location.client.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class Sidebar extends JPanel {
  public interface NavListener {
    void onNavigate(String target);
  }

  private record Entry(String id, String label) {}

  private final NavListener listener;
  private final List<Entry> entries = new ArrayList<>();
  private final Map<String, IconBadgeButton> items = new LinkedHashMap<>();

  public Sidebar(NavListener listener) {
    super(new GridLayout(0, 1, 0, 4));
    this.listener = listener;
    setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    setBackground(UIManager.getColor("Panel.background"));
    addItem("planning", "Planning", IconLoader.planning());
    addItem("clients", "Clients", IconLoader.clients());
    addItem("resources", "Ressources", IconLoader.resources());
    addItem("drivers", "Chauffeurs", IconLoader.drivers());
    addItem("docs", "Documents", IconLoader.docs());
    addItem("unav", "Indispos", IconLoader.unavailabilities());
  }

  private void addItem(String id, String label, Icon icon) {
    IconBadgeButton button = new IconBadgeButton(label, icon);
    button.addActionListener(
        e -> {
          if (listener != null) {
            listener.onNavigate(id);
          }
        });
    button.setToolTipText(label + "  (Alt+" + (entries.size() + 1) + ")");
    items.put(id, button);
    entries.add(new Entry(id, label));
    add(button);
  }

  public void setSelected(String id) {
    items.forEach((key, button) -> button.setEnabled(!key.equals(id)));
  }

  public void setBadge(String id, int count) {
    IconBadgeButton button = items.get(id);
    if (button != null) {
      button.setBadgeCount(count);
    }
  }

  public int indexOf(String id) {
    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i).id().equals(id)) {
        return i;
      }
    }
    return -1;
  }

  public String idAt(int index) {
    if (index < 0 || index >= entries.size()) {
      return null;
    }
    return entries.get(index).id();
  }

  public int entryCount() {
    return entries.size();
  }

  private static class IconBadgeButton extends JButton {
    private int badgeCount;

    IconBadgeButton(String text, Icon icon) {
      super(text, icon);
      putClientProperty("JButton.buttonType", "toolBarButton");
      setHorizontalAlignment(SwingConstants.LEFT);
      setFocusable(false);
      setIconTextGap(10);
      setMargin(new Insets(6, 8, 6, 8));
    }

    void setBadgeCount(int badgeCount) {
      if (this.badgeCount != badgeCount) {
        this.badgeCount = badgeCount;
        repaint();
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (badgeCount <= 0) {
        return;
      }
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int radius = 16;
      int x = getWidth() - radius - 8;
      int y = 6;
      g2.setColor(new Color(220, 30, 35));
      g2.fillOval(x, y, radius, radius);
      g2.setColor(Color.WHITE);
      g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 11f));
      String text = String.valueOf(badgeCount);
      java.awt.FontMetrics fm = g2.getFontMetrics();
      int textX = x + (radius - fm.stringWidth(text)) / 2;
      int textY = y + (radius + fm.getAscent() - fm.getDescent()) / 2 - 1;
      g2.drawString(text, textX, textY);
      g2.dispose();
    }
  }
}
