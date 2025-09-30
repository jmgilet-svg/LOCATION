package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import com.location.client.ui.uikit.Toasts;

public class DocTemplatesWysiwygFrame extends JFrame {
  private final DataSourceProvider dsp;
  private final JComboBox<String> cbType = new JComboBox<>(new String[]{"QUOTE", "ORDER", "DELIVERY", "INVOICE"});
  private final HtmlEditorPanel editor = new HtmlEditorPanel();

  public DocTemplatesWysiwygFrame(DataSourceProvider dsp) {
    super("Modèles document — WYSIWYG");
    this.dsp = dsp;
    setLayout(new BorderLayout(6, 6));
    JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
    north.add(new JLabel("Type :"));
    north.add(cbType);
    JButton btLoad = new JButton(new AbstractAction("Charger") {
      @Override
      public void actionPerformed(ActionEvent e) {
        load();
      }
    });
    JButton btSave = new JButton(new AbstractAction("Enregistrer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        save();
      }
    });
    JButton btPreview = new JButton(new AbstractAction("Aperçu") {
      @Override
      public void actionPerformed(ActionEvent e) {
        preview();
      }
    });
    north.add(btLoad);
    north.add(btSave);
    north.add(btPreview);
    add(north, BorderLayout.NORTH);
    add(editor, BorderLayout.CENTER);
    setSize(1000, 700);
    setLocationRelativeTo(null);
    load();
  }

  private void load() {
    String type = (String) cbType.getSelectedItem();
    Models.DocTemplate t = dsp.getDocTemplate(type);
    editor.setHtml(t != null ? t.html() : "<h1>" + type + "</h1><p>Contenu…</p>");
  }

  private void save() {
    String type = (String) cbType.getSelectedItem();
    dsp.saveDocTemplate(type, editor.getHtml());
    Toasts.success(this, "Modèle " + type + " enregistré");
  }

  private void preview() {
    JDialog d = new JDialog(this, "Aperçu", Dialog.ModalityType.MODELESS);
    JEditorPane pane = new JEditorPane("text/html", editor.getHtml());
    pane.setEditable(false);
    d.add(new JScrollPane(pane));
    d.setSize(800, 600);
    d.setLocationRelativeTo(this);
    d.setVisible(true);
  }
}
