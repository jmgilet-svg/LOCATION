package com.location.client.ui.uikit;

import javax.swing.*;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

public final class AgencyPalette {
  private static final String PREF_NODE = "com.location.ui.agency.palette";
  private static final Pattern HEX = Pattern.compile("^#?[0-9a-fA-F]{6}$");
  private AgencyPalette(){}

  public static void set(String agencyId, String hex){
    if (agencyId == null || agencyId.isBlank()) return;
    if (hex == null || !HEX.matcher(hex).matches()) return;
    if (!hex.startsWith("#")) hex = "#"+hex;
    prefs().put(key(agencyId), hex);
  }

  public static String get(String agencyId){
    if (agencyId == null) return null;
    return prefs().get(key(agencyId), null);
  }

  public static void clear(String agencyId){
    if (agencyId == null) return;
    prefs().remove(key(agencyId));
  }

  public static void applyForAgency(String agencyId){
    String hex = get(agencyId);
    if (hex == null) hex = "#3D7EFF";
    ThemePalette.apply(hex);
    SwingUtilities.invokeLater(() -> {
      java.awt.Window w = javax.swing.FocusManager.getCurrentManager().getActiveWindow();
      if (w != null) javax.swing.SwingUtilities.updateComponentTreeUI(w);
    });
  }

  private static String key(String id){ return "accent_"+id; }
  private static Preferences prefs(){ return Preferences.userRoot().node(PREF_NODE); }
}
