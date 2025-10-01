package com.location.client.ui.uikit;

import javax.swing.*;
import java.awt.*;

public final class ThemePalette {
  private ThemePalette(){}

  public static void apply(String accentHex){
    Color accent = parseColor(accentHex, new Color(0,120,215));
    UIManager.put("Component.arc", 16);
    UIManager.put("Button.arc", 18);
    UIManager.put("TextComponent.arc", 14);
    UIManager.put("ScrollBar.thumbArc", 14);
    UIManager.put("Component.focusWidth", 1);
    UIManager.put("Component.innerFocusWidth", 0);
    UIManager.put("Component.focusColor", accent);
    UIManager.put("Button.focusedBorderColor", accent);
    UIManager.put("CheckBox.icon.focusColor", accent);
    UIManager.put("RadioButton.icon.focusColor", accent);
    UIManager.put("TabbedPane.focusColor", accent);
    UIManager.put("Button.hoverBackground", withAlpha(accent, 30));
    UIManager.put("Button.pressedBackground", withAlpha(accent, 60));
    UIManager.put("TextComponent.selectionBackground", withAlpha(accent, 90));
    UIManager.put("Table.selectionBackground", withAlpha(accent, 80));
    UIManager.put("List.selectionBackground", withAlpha(accent, 80));
  }

  private static Color parseColor(String hex, Color def){
    try {
      return Color.decode(hex);
    } catch (Exception e){
      return def;
    }
  }

  private static Color withAlpha(Color c, int a){
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
  }
}
