package com.location.client.ui.uikit;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.lang.reflect.Constructor;
import java.net.URL;

public final class Svg {
  private Svg() {}

  public static Icon icon(String name, int size) {
    String svgPath = "/icons/" + name + ".svg";
    URL svgUrl = Svg.class.getResource(svgPath);
    if (svgUrl != null) {
      try {
        Class<?> cls = Class.forName("com.formdev.flatlaf.extras.FlatSVGIcon");
        Constructor<?> ctor = cls.getConstructor(URL.class, int.class, int.class);
        return (Icon) ctor.newInstance(svgUrl, size, size);
      } catch (Throwable ignore) {
      }
    }
    String pngPath = "/icons/" + name + ".png";
    URL pngUrl = Svg.class.getResource(pngPath);
    if (pngUrl != null) {
      return new ImageIcon(pngUrl);
    }
    return new EmptyIcon(size, size);
  }

  public static final class EmptyIcon implements Icon {
    private final int width;
    private final int height;

    public EmptyIcon(int width, int height) {
      this.width = width;
      this.height = height;
    }

    @Override
    public int getIconWidth() {
      return width;
    }

    @Override
    public int getIconHeight() {
      return height;
    }

    @Override
    public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
      // no-op
    }
  }
}
