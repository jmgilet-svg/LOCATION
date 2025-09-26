package com.location.client.ui;

import java.awt.Dimension;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JTextArea;

public class CheatSheetDialog extends JDialog {
  public CheatSheetDialog(Window owner) {
    super(owner, "Raccourcis clavier", ModalityType.MODELESS);
    JTextArea shortcuts =
        new JTextArea(
            """
RACCOURCIS — PLANNING

Ctrl+N     : Nouvelle intervention (08:00–10:00)
Ctrl+D     : Dupliquer la sélection (+1h)
Suppr      : Supprimer la sélection
Alt+←/→    : Jour précédent / suivant
Ctrl+K     : Palette de commandes
Ctrl+F     : Recherche globale
Ctrl+Alt+L : Thème clair
Ctrl+Alt+D : Thème sombre
Double‑clic: Édition rapide (titre, heures, ressource)
""");
    shortcuts.setEditable(false);
    shortcuts.setOpaque(false);
    shortcuts.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    add(shortcuts);
    setPreferredSize(new Dimension(440, 260));
    pack();
    setLocationRelativeTo(owner);
  }
}
