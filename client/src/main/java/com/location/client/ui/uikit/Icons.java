package com.location.client.ui.uikit;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.Icon;
import javax.swing.UIManager;

public final class Icons {
  public static final String CALENDAR = "calendar-event";
  public static final String FILE_TEXT = "file-text";
  public static final String PDF = "file-type-pdf";
  public static final String SEND = "send";
  public static final String CLIENTS = "users";
  public static final String RESOURCES = "boxes";
  public static final String DRIVERS = "truck";
  public static final String CONFLICT = "alert-octagon";
  public static final String CHECK = "check-circle";
  public static final String SEARCH = "search";
  public static final String SETTINGS = "settings";

  private Icons() {}

  public static Icon of(String name, int size) {
    try {
      return new FlatSVGIcon("icons/" + name + ".svg", size, size);
    } catch (Exception ex) {
      Icon fallback = Svg.icon(name, size);
      if (fallback != null) {
        return fallback;
      }
      Icon fileIcon = UIManager.getIcon("FileView.fileIcon");
      return fileIcon != null ? fileIcon : new Svg.EmptyIcon(size, size);
    }
  }
}
