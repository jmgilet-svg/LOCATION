package com.location.client.ui;

import com.location.client.ui.uikit.Icons;
import com.location.client.ui.uikit.Svg;
import javax.swing.Icon;

public final class IconLoader {
  private IconLoader() {}

  public static Icon planning() {
    return svg(Icons.CALENDAR);
  }

  public static Icon clients() {
    return svg(Icons.CLIENTS);
  }

  public static Icon resources() {
    return svg(Icons.RESOURCES);
  }

  public static Icon drivers() {
    return svg(Icons.DRIVERS);
  }

  public static Icon docs() {
    return svg(Icons.FILE_TEXT);
  }

  public static Icon unavailabilities() {
    return svg("pause");
  }

  public static Icon reports() {
    return svg("chart");
  }

  public static Icon search() {
    return svg(Icons.SEARCH);
  }

  public static Icon lightning() {
    return svg("bolt");
  }

  public static Icon settings() {
    return svg(Icons.SETTINGS);
  }

  private static Icon svg(String name) {
    return Svg.icon(name, 16);
  }
}
