package com.location.client.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.Icon;

public final class IconLoader {
  private IconLoader() {}

  public static Icon planning() {
    return svg("calendar.svg");
  }

  public static Icon clients() {
    return svg("users.svg");
  }

  public static Icon resources() {
    return svg("toolbox.svg");
  }

  public static Icon drivers() {
    return svg("steering.svg");
  }

  public static Icon docs() {
    return svg("docs.svg");
  }

  public static Icon unavailabilities() {
    return svg("pause.svg");
  }

  public static Icon reports() {
    return svg("chart.svg");
  }

  public static Icon search() {
    return svg("search.svg");
  }

  public static Icon lightning() {
    return svg("bolt.svg");
  }

  public static Icon settings() {
    return svg("settings.svg");
  }

  private static Icon svg(String name) {
    return new FlatSVGIcon("icons/" + name, 16, 16);
  }
}
