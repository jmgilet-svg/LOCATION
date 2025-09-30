package com.location.client.ui.uikit;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JToggleButton;

public class Chip extends JToggleButton {
  public Chip(String label) {
    super(label);
    setFocusPainted(false);
    setBorderPainted(false);
    setContentAreaFilled(false);
    setMargin(new Insets(4, 10, 4, 10));
    setOpaque(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g2 = (Graphics2D) graphics.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int arc = 16;
    Color border = isSelected() ? new Color(59, 130, 246) : new Color(203, 213, 225);
    Color fill = isSelected() ? new Color(59, 130, 246, 40) : new Color(203, 213, 225, 40);
    g2.setColor(fill);
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
    g2.setColor(border);
    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
    g2.dispose();
    super.paintComponent(graphics);
  }
}
