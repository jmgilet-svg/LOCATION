package com.location.client.ui.uikit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class EmptyState extends JPanel {
  public EmptyState(Icon icon, String title, String description) {
    super(new BorderLayout());
    setOpaque(false);
    JPanel center = new JPanel();
    center.setOpaque(false);
    center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

    JLabel iconLabel = new JLabel(icon);
    iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
    titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel descriptionLabel =
        new JLabel("<html><div style='text-align:center;'>" + description + "</div></html>");
    descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    descriptionLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

    center.add(iconLabel);
    center.add(Box.createVerticalStrut(10));
    center.add(titleLabel);
    center.add(Box.createVerticalStrut(6));
    center.add(descriptionLabel);
    add(center, BorderLayout.CENTER);
    setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
  }
}
