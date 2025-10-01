package com.location.client.ui.uikit;

import javax.swing.*;
import java.awt.*;

public final class ThemePalette {
  private ThemePalette(){}
  public static void apply(String accentHex){
    Color accent = parse(accentHex, new Color(0,122,255));
    UIManager.put("Component.arc", 16);
    UIManager.put("Button.arc", 18);
    UIManager.put("TextComponent.arc", 14);
    UIManager.put("ScrollBar.thumbArc", 14);
    UIManager.put("Component.focusColor", accent);
    UIManager.put("Button.hoverBackground", withAlpha(accent, 30));
    UIManager.put("Button.pressedBackground", withAlpha(accent, 60));
    UIManager.put("List.selectionBackground", withAlpha(accent, 80));
    UIManager.put("Table.selectionBackground", withAlpha(accent, 80));
    UIManager.put("TextComponent.selectionBackground", withAlpha(accent, 90));
  }
  private static Color parse(String hex, Color def){ try { return Color.decode(hex); } catch(Exception e){ return def; } }
  private static Color withAlpha(Color c, int a){ return new Color(c.getRed(),c.getGreen(),c.getBlue(), Math.max(0, Math.min(255,a))); }
}
