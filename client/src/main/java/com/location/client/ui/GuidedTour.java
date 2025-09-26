package com.location.client.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.KeyStroke;

/** Overlay très léger pour un tour guidé étape par étape. */
public final class GuidedTour extends JComponent {
  public record Step(Supplier<Rectangle> area, String title, String body) {}

  private final List<Step> steps = new ArrayList<>();
  private final JLayeredPane host;
  private final Runnable onFinish;
  private int index;

  public GuidedTour(JLayeredPane host, Runnable onFinish) {
    this.host = host;
    this.onFinish = onFinish;
    setOpaque(false);
    setFocusable(true);
    registerKeyboardAction(
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            next();
          }
        },
        KeyStroke.getKeyStroke("ENTER"),
        WHEN_IN_FOCUSED_WINDOW);
    registerKeyboardAction(
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            finish();
          }
        },
        KeyStroke.getKeyStroke("ESCAPE"),
        WHEN_IN_FOCUSED_WINDOW);
  }

  public GuidedTour addStep(Supplier<Rectangle> area, String title, String body) {
    steps.add(new Step(area, title, body));
    return this;
  }

  public void start() {
    setBounds(0, 0, host.getWidth(), host.getHeight());
    host.add(this, JLayeredPane.DRAG_LAYER);
    host.revalidate();
    host.repaint();
    requestFocusInWindow();
  }

  @Override
  public void addNotify() {
    super.addNotify();
    Component parent = getParent();
    if (parent != null) {
      parent.addComponentListener(
          new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
              setBounds(0, 0, host.getWidth(), host.getHeight());
              repaint();
            }
          });
    }
  }

  private void next() {
    index++;
    if (index >= steps.size()) {
      finish();
    } else {
      repaint();
    }
  }

  private void finish() {
    if (getParent() != null) {
      host.remove(this);
      host.revalidate();
      host.repaint();
    }
    if (onFinish != null) {
      onFinish.run();
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setColor(new Color(0, 0, 0, 170));
    g2.fillRect(0, 0, getWidth(), getHeight());
    if (index < steps.size()) {
      Step step = steps.get(index);
      Rectangle area = step.area().get();
      if (area != null) {
        Rectangle padded = new Rectangle(area);
        padded.grow(12, 12);
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRoundRect(padded.x, padded.y, padded.width, padded.height, 18, 18);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(new Color(255, 255, 255, 235));
        int boxWidth = Math.min(getWidth() - 40, 420);
        int boxX = Math.max(20, Math.min(getWidth() - boxWidth - 20, padded.x + padded.width + 20));
        int boxY = Math.max(20, padded.y);
        g2.fillRoundRect(boxX, boxY, boxWidth, 150, 16, 16);
        g2.setColor(new Color(45, 45, 45));
        g2.setFont(getFont().deriveFont(getFont().getSize2D() + 1f));
        g2.drawString(step.title(), boxX + 14, boxY + 24);
        g2.setFont(getFont());
        drawWrapped(g2, step.body(), boxX + 14, boxY + 48, boxWidth - 28);
        g2.setColor(new Color(90, 90, 90));
        g2.drawString("[Entrée] Suivant   [Échap] Terminer", boxX + 14, boxY + 132);
      }
    }
    g2.dispose();
  }

  private void drawWrapped(Graphics2D g2, String text, int x, int y, int width) {
    if (text == null || text.isBlank()) {
      return;
    }
    java.awt.FontMetrics fm = g2.getFontMetrics();
    String[] words = text.split(" ");
    StringBuilder line = new StringBuilder();
    for (String word : words) {
      String candidate = line.isEmpty() ? word : line + " " + word;
      if (fm.stringWidth(candidate) > width) {
        g2.drawString(line.toString(), x, y);
        y += fm.getHeight();
        line = new StringBuilder(word);
      } else {
        line = new StringBuilder(candidate);
      }
    }
    if (!line.isEmpty()) {
      g2.drawString(line.toString(), x, y);
    }
  }
}
