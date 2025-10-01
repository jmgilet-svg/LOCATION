package com.location.client.ui.uikit;

import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public final class Theme {
  private Theme() {}

  public static JMenu buildViewMenu(JFrame frame) {
    JMenu view = new JMenu("Affichage");
    attach(view, frame);
    return view;
  }

  public static void attach(JMenu view, JFrame frame) {
    if (view == null) {
      return;
    }
    if (view.getMenuComponentCount() > 0) {
      view.addSeparator();
    }

    JMenu themeMenu = new JMenu("Thème");
    ButtonGroup group = new ButtonGroup();
    JRadioButtonMenuItem light = new JRadioButtonMenuItem("Clair");
    JRadioButtonMenuItem dark = new JRadioButtonMenuItem("Sombre");
    group.add(light);
    group.add(dark);

    light.addActionListener(e -> apply(frame, com.location.client.ui.Theme.Mode.LIGHT));
    dark.addActionListener(e -> apply(frame, com.location.client.ui.Theme.Mode.DARK));

    themeMenu.add(light);
    themeMenu.add(dark);
    view.add(themeMenu);

    view.addMenuListener(
        new MenuListener() {
          @Override
          public void menuSelected(MenuEvent e) {
            com.location.client.ui.Theme.Mode mode = com.location.client.ui.Theme.currentMode();
            light.setSelected(mode == com.location.client.ui.Theme.Mode.LIGHT);
            dark.setSelected(mode == com.location.client.ui.Theme.Mode.DARK);
          }

          @Override
          public void menuDeselected(MenuEvent e) {}

          @Override
          public void menuCanceled(MenuEvent e) {}
        });

    view.addSeparator();

    JMenuItem bigger = new JMenuItem("Augmenter la taille UI");
    bigger.addActionListener(
        e -> adjustScale(frame, com.location.client.ui.Theme.getFontScale() + 0.1f));

    JMenuItem smaller = new JMenuItem("Diminuer la taille UI");
    smaller.addActionListener(
        e -> adjustScale(frame, com.location.client.ui.Theme.getFontScale() - 0.1f));

    JMenuItem reset = new JMenuItem("Taille par défaut");
    reset.addActionListener(e -> adjustScale(frame, 1.0f));

    view.add(bigger);
    view.add(smaller);
    view.add(reset);
  }

  private static void apply(JFrame frame, com.location.client.ui.Theme.Mode mode) {
    animate(() -> com.location.client.ui.Theme.apply(mode));
  }

  private static void adjustScale(JFrame frame, float scale) {
    animate(() -> com.location.client.ui.Theme.setFontScale(scale));
  }

  private static void animate(Runnable action) {
    FlatAnimatedLafChange.showSnapshot();
    try {
      action.run();
    } finally {
      FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }
  }
}
