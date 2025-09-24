package com.location.client.core;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.*;

public class SelectionDialog {
  public record Selection(String mode, boolean remember) {}

  public static Selection showAndGet() {
    JDialog d = new JDialog((JFrame) null, "Sélection de la source de données", true);
    JPanel root = new JPanel(new BorderLayout(10, 10));
    root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    JLabel title =
        new JLabel(
            "<html><h2>Choisissez la source de données</h2><p>Mode Démo (Mock) — aucune connexion réseau<br/>Mode Connecté (Backend) — REST + JWT</p></html>");
    root.add(title, BorderLayout.NORTH);

    JPanel center = new JPanel(new GridLayout(1, 2, 12, 12));
    JButton mockBtn = new JButton("Mode Démo (Mock)");
    JButton restBtn = new JButton("Mode Connecté (Backend)");
    center.add(mockBtn);
    center.add(restBtn);
    root.add(center, BorderLayout.CENTER);

    JCheckBox remember = new JCheckBox("Mémoriser ce choix");
    root.add(remember, BorderLayout.SOUTH);

    final String[] chosen = {null};
    mockBtn.addActionListener(
        e -> {
          chosen[0] = "mock";
          d.dispose();
        });
    restBtn.addActionListener(
        e -> {
          chosen[0] = "rest";
          d.dispose();
        });

    d.setContentPane(root);
    d.pack();
    d.setLocationRelativeTo(null);
    d.setVisible(true);

    if (chosen[0] == null) return null;
    return new Selection(chosen[0], remember.isSelected());
  }
}
