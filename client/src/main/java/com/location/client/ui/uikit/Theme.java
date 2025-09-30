package com.location.client.ui.uikit;

import javax.swing.UIManager;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.io.InputStream;
import java.lang.reflect.Method;

public final class Theme {
  private Theme() {}

  public static void applyLight() {
    apply("com.formdev.flatlaf.FlatLightLaf");
  }

  public static void applyDark() {
    apply("com.formdev.flatlaf.FlatDarkLaf");
  }

  private static void apply(String lafClassName) {
    try {
      Class<?> laf = Class.forName(lafClassName);
      Method setup = laf.getMethod("setup");
      setup.invoke(null);
    } catch (Throwable t) {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception ignore) {
      }
    }
    installInterIfPresent();
    UIManager.put("Component.arc", 12);
    UIManager.put("Button.arc", 12);
    UIManager.put("TextComponent.arc", 12);
    UIManager.put("ScrollBar.showButtons", Boolean.FALSE);
    UIManager.put("ToolTip.hideAccelerator", Boolean.TRUE);
    UIManager.put("Button.margin", new Insets(6, 10, 6, 10));
    UIManager.put("Component.arrowType", "chevron");
  }

  private static void installInterIfPresent() {
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");
    String[] faces = {
      "/fonts/Inter-Regular.ttf",
      "/fonts/Inter-Medium.ttf",
      "/fonts/Inter-SemiBold.ttf"
    };
    for (String face : faces) {
      try (InputStream in = Theme.class.getResourceAsStream(face)) {
        if (in != null) {
          Font font = Font.createFont(Font.TRUETYPE_FONT, in);
          GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        }
      } catch (Exception ignore) {
      }
    }
    Font base = new Font("Inter", Font.PLAIN, 13);
    UIManager.put("defaultFont", base);
  }
}
