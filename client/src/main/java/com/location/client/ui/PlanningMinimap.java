package com.location.client.ui;

import com.location.client.core.Models;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.JComponent;

/**
 * Minimap simplifiée affichant les interventions de la journée et le viewport courant.
 */
public class PlanningMinimap extends JComponent {
  private List<Models.Intervention> interventions = List.of();
  private int openHour = 6;
  private int closeHour = 20;
  private Rectangle2D viewportRatio = new Rectangle2D.Double(0, 0, 1, 1);

  public PlanningMinimap() {
    setPreferredSize(new Dimension(100, 48));
    setOpaque(false);
  }

  public void setInterventions(List<Models.Intervention> interventions) {
    this.interventions =
        interventions == null ? List.of() : List.copyOf(interventions);
    repaint();
  }

  public void setWorkingHours(int openHour, int closeHour) {
    if (closeHour <= openHour) {
      return;
    }
    this.openHour = openHour;
    this.closeHour = closeHour;
    repaint();
  }

  public void setViewportRatio(Rectangle2D ratio) {
    if (ratio == null) {
      viewportRatio = new Rectangle2D.Double(0, 0, 1, 1);
    } else {
      viewportRatio =
          new Rectangle2D.Double(
              clamp(ratio.getX(), 0, 1),
              clamp(ratio.getY(), 0, 1),
              clamp(ratio.getWidth(), 0, 1),
              clamp(ratio.getHeight(), 0, 1));
    }
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int w = getWidth();
      int h = getHeight();
      if (w <= 0 || h <= 0) {
        return;
      }

      g2.setColor(new Color(30, 30, 30, 30));
      g2.fillRect(0, 0, w, h);
      g2.setColor(new Color(255, 255, 255, 60));
      g2.fillRect(0, 0, w, h);

      int totalSeconds = Math.max(1, (closeHour - openHour) * 3600);
      ZoneId zone = ZoneId.systemDefault();
      List<String> resourceOrder =
          interventions.stream()
              .map(Models.Intervention::resourceId)
              .map(id -> id == null ? "" : id)
              .distinct()
              .collect(Collectors.toList());
      if (resourceOrder.isEmpty()) {
        resourceOrder = List.of("");
      }
      int lanes = resourceOrder.size();
      int laneHeight = Math.max(3, (h - 4) / lanes);

      for (Models.Intervention intervention : interventions) {
        if (intervention.start() == null || intervention.end() == null) {
          continue;
        }
        ZonedDateTime start = intervention.start().atZone(zone);
        ZonedDateTime end = intervention.end().atZone(zone);
        double startSeconds =
            (start.getHour() - openHour) * 3600d
                + start.getMinute() * 60d
                + start.getSecond();
        double endSeconds =
            (end.getHour() - openHour) * 3600d
                + end.getMinute() * 60d
                + end.getSecond();
        double startRatio = clamp(startSeconds / totalSeconds, 0d, 1d);
        double endRatio = clamp(endSeconds / totalSeconds, 0d, 1d);
        int x = (int) Math.round(startRatio * w);
        int width = (int) Math.round((endRatio - startRatio) * w);
        if (width <= 2) {
          width = 2;
        }
        String resourceId = Objects.requireNonNullElse(intervention.resourceId(), "");
        int laneIndex = resourceOrder.indexOf(resourceId);
        if (laneIndex < 0) {
          laneIndex = 0;
        }
        int y = 2 + laneIndex * laneHeight;
        int rectHeight = Math.min(laneHeight - 2, 6);
        rectHeight = Math.max(rectHeight, 3);
        g2.setColor(new Color(80, 150, 255, 140));
        g2.fillRect(x, y, width, rectHeight);
      }

      g2.setColor(new Color(20, 20, 20, 150));
      g2.setStroke(new BasicStroke(1.5f));
      int vx = (int) Math.round(viewportRatio.getX() * w);
      int vy = (int) Math.round(viewportRatio.getY() * h);
      int vw = (int) Math.round(viewportRatio.getWidth() * w);
      int vh = (int) Math.round(viewportRatio.getHeight() * h);
      vw = Math.max(4, Math.min(vw, w));
      vh = Math.max(4, Math.min(vh, h));
      vx = Math.max(0, Math.min(vx, w - 1));
      vy = Math.max(0, Math.min(vy, h - 1));
      g2.drawRect(vx, vy, Math.min(vw, w - vx - 1), Math.min(vh, h - vy - 1));
    } finally {
      g2.dispose();
    }
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
