package com.location.client.ui.uikit;

import javax.swing.*;
import java.awt.*;

public final class MicroAnimations {
  private MicroAnimations(){}

  /** Simple pulsing border around a component. Call stop() on the returned timer to end. */
  public static Timer pulseBorder(JComponent c){
    long start = System.currentTimeMillis();
    Timer t = new Timer(40, e -> c.repaint());
    c.putClientProperty("pulse.start", start);
    t.start();
    c.addPropertyChangeListener("pulse.active", evt -> { if(Boolean.FALSE.equals(evt.getNewValue())) t.stop(); });
    return t;
  }

  /** Paint a pulse using this in component's paint after super.paint(g). */
  public static void paintPulse(JComponent c, Graphics g){
    Object v = c.getClientProperty("pulse.start");
    if (!(v instanceof Long)) return;
    long start = (Long) v;
    long dt = System.currentTimeMillis() - start;
    float phase = (float)Math.abs(Math.sin(dt/500.0));
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f + 0.35f*phase));
    g2.setColor(new Color(61,126,255));
    g2.setStroke(new BasicStroke(2f));
    g2.drawRect(1,1,c.getWidth()-3,c.getHeight()-3);
    g2.dispose();
  }
}
