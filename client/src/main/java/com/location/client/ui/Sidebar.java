package com.location.client.ui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class Sidebar extends JPanel {
  public interface NavListener {
    void onNavigate(String target);
  }

  private final NavListener listener;
  private final Map<String, JButton> items = new LinkedHashMap<>();

  public Sidebar(NavListener listener) {
    super(new GridLayout(0, 1, 0, 4));
    this.listener = listener;
    setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    setBackground(UIManager.getColor("Panel.background"));
    addItem("planning", "Planning", IconLoader.planning());
    addItem("clients", "Clients", IconLoader.clients());
    addItem("resources", "Ressources", IconLoader.resources());
    addItem("drivers", "Chauffeurs", IconLoader.drivers());
    addItem("docs", "Documents", IconLoader.docs());
    addItem("unav", "Indispos", IconLoader.unavailabilities());
    addItem("reports", "Rapports", IconLoader.reports());
  }

  private void addItem(String id, String label, Icon icon) {
    JButton button = new JButton(new AbstractAction(label, icon) {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (listener != null) {
          listener.onNavigate(id);
        }
        setSelected(id);
      }
    });
    button.putClientProperty("JButton.buttonType", "toolBarButton");
    button.setHorizontalAlignment(SwingConstants.LEFT);
    button.setFocusable(false);
    items.put(id, button);
    add(button);
  }

  public void setSelected(String id) {
    items.forEach((key, button) -> button.setEnabled(!key.equals(id)));
  }
}
