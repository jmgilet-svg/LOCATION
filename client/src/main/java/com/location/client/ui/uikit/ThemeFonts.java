package com.location.client.ui.uikit;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public final class ThemeFonts {
  private ThemeFonts(){}

  public static void applyDefaultFont() {
    Font base = tryLoad("/fonts/Inter-Regular.ttf");
    if (base == null) base = tryLoad("/fonts/Roboto-Regular.ttf");
    if (base == null) base = new Font("SansSerif", Font.PLAIN, 13);
    final Font f = base.deriveFont(13f);
    UIManager.getLookAndFeelDefaults().forEach((k, v) -> {
      if (v instanceof Font) UIManager.put(k, f);
    });
  }

  private static Font tryLoad(String res){
    try (InputStream in = ThemeFonts.class.getResourceAsStream(res)){
      if (in == null) return null;
      Font f = Font.createFont(Font.TRUETYPE_FONT, in);
      GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
      return f;
    } catch (Exception ignore){
      return null;
    }
  }
}
