package com.location.client.ui;

import com.location.client.ui.uikit.Icons;
import com.location.client.ui.uikit.Theme;
import com.location.client.ui.uikit.ThemeFonts;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PlanningToolbar extends JToolBar {
  private final PlanningPanel planning;

  public PlanningToolbar(PlanningPanel planning){
    super("Outils");
    this.planning = planning;
    setFloatable(false);
    setBorder(BorderFactory.createEmptyBorder(4,6,4,6));
    build();
  }

  private void build(){
    add(action("Annuler", Icons.of("undo", 18), e -> planning.undoLast()));
    add(action("Rétablir", Icons.of("redo", 18), e -> planning.redoLast()));
    addSeparator();

    add(action("Dupliquer +1j", Icons.of("duplicate", 18), e -> planning.duplicateSelected(1)));
    add(action("Dupliquer +7j", Icons.of("calendar", 18), e -> planning.duplicateSelected(7)));
    addSeparator();

    add(action("Vue jour", Icons.of("day", 18), e -> planning.setWeekMode(false)));
    add(action("Vue semaine", Icons.of("week", 18), e -> planning.setWeekMode(true)));
    addSeparator();

    JTextField tagQuick = new JTextField(14);
    tagQuick.putClientProperty("JTextField.placeholderText", "Ajouter tag rapide…");
    tagQuick.addActionListener(ev -> {
      String t = tagQuick.getText().trim();
      if (!t.isEmpty()) planning.addQuickTagToSelection(t);
      tagQuick.setText("");
    });
    add(new JLabel(Icons.of("tag", 16)));
    add(tagQuick);
    addSeparator();

    add(action("Thème", Icons.of("palette", 18), e -> Theme.apply(nextMode(), getScale(), true)));
    add(action("Taille police", Icons.of("font", 18), e -> {
      ThemeFonts.applyDefaultFont();
      SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(this));
    }));
  }

  private Action action(String name, Icon icon, java.util.function.Consumer<ActionEvent> cb){
    return new AbstractAction(name, icon){
      @Override public void actionPerformed(ActionEvent e){ cb.accept(e); }
    };
  }

  private String nextMode(){
    return com.location.client.ui.Theme.currentMode() == com.location.client.ui.Theme.Mode.LIGHT
        ? "dark"
        : "light";
  }

  private float getScale(){
    return com.location.client.ui.Theme.getFontScale();
  }
}
