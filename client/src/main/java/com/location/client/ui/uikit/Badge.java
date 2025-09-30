package com.location.client.ui.uikit;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

public class Badge extends JLabel {
  public enum Variant {
    PRIMARY,
    SUCCESS,
    WARNING,
    DANGER,
    NEUTRAL
  }

  private Variant variant = Variant.NEUTRAL;

  public Badge(String text, Variant variant) {
    super(text);
    setOpaque(false);
    setFont(getFont().deriveFont(Font.BOLD, 11f));
    setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
    this.variant = variant == null ? Variant.NEUTRAL : variant;
    setForeground(Color.DARK_GRAY);
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    Color background;
    switch (variant) {
      case PRIMARY -> background = new Color(30, 144, 255, 40);
      case SUCCESS -> background = new Color(16, 185, 129, 40);
      case WARNING -> background = new Color(245, 158, 11, 40);
      case DANGER -> background = new Color(239, 68, 68, 40);
      default -> background = new Color(107, 114, 128, 40);
    }
    int arc = 16;
    g2.setColor(background);
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
    g2.dispose();
    super.paintComponent(g);
  }
}
