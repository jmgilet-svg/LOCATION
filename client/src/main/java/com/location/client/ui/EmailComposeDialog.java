package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/** Composition e‑mail groupée avec autocomplete basique sur les clients connus. */
public class EmailComposeDialog extends JDialog {
  private final DataSourceProvider dsp;
  private final JTextField tfIds = new JTextField(28);
  private final JComboBox<String> cbTo = new JComboBox<>();
  private final JTextField tfSubject = new JTextField(32);
  private final HtmlEditorPanel editor = new HtmlEditorPanel();
  private final JCheckBox cbAttachPdf = new JCheckBox("Joindre le PDF", true);

  public EmailComposeDialog(Window owner, DataSourceProvider dsp, List<String> preselectedDocIds) {
    super(owner, "Envoyer par e‑mail", ModalityType.APPLICATION_MODAL);
    this.dsp = dsp;
    setLayout(new BorderLayout(6, 6));

    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(4, 6, 4, 6);
    c.anchor = GridBagConstraints.WEST;
    int y = 0;
    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("IDs documents (séparés par ,)"), c);
    c.gridx = 1;
    tfIds.setText(preselectedDocIds == null ? "" : String.join(",", preselectedDocIds));
    form.add(tfIds, c);
    y++;

    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("À"), c);
    c.gridx = 1;
    cbTo.setEditable(true);
    fillEmails();
    form.add(cbTo, c);
    y++;

    c.gridx = 0;
    c.gridy = y;
    form.add(new JLabel("Sujet"), c);
    c.gridx = 1;
    form.add(tfSubject, c);
    y++;
    add(form, BorderLayout.NORTH);

    add(editor, BorderLayout.CENTER);

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    south.add(cbAttachPdf);
    south.add(new JButton(new AbstractAction("Annuler") {
      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    }));
    south.add(new JButton(new AbstractAction("Envoyer") {
      @Override
      public void actionPerformed(ActionEvent e) {
        send();
      }
    }));
    add(south, BorderLayout.SOUTH);
    setSize(900, 640);
    setLocationRelativeTo(owner);
  }

  private void fillEmails() {
    cbTo.removeAllItems();
    for (Models.Client c : dsp.listClients()) {
      if (c.billingEmail() != null && !c.billingEmail().isBlank()) {
        cbTo.addItem(c.name() + " <" + c.billingEmail() + ">");
      }
    }
  }

  private void send() {
    try {
      List<String> ids = parseIds(tfIds.getText());
      String to = (String) (cbTo.isEditable() ? cbTo.getEditor().getItem() : cbTo.getSelectedItem());
      String subject = tfSubject.getText().isBlank() ? null : tfSubject.getText();
      String body = editor.getHtml().isBlank() ? null : editor.getHtml();
      if (ids.isEmpty()) {
        throw new IllegalArgumentException("Renseignez au moins un ID de document.");
      }
      if (to == null || to.isBlank()) {
        throw new IllegalArgumentException("Renseignez un destinataire.");
      }
      dsp.emailDocsBatch(ids, to, subject, body, cbAttachPdf.isSelected());
      Toast.success(this, "E‑mail envoyé (" + ids.size() + " doc)");
      ActivityCenter.log("Email groupé → " + to + " (" + ids.size() + ")");
      dispose();
    } catch (RuntimeException ex) {
      Toast.error(this, ex.getMessage());
    }
  }

  private static List<String> parseIds(String s) {
    if (s == null || s.isBlank()) {
      return List.of();
    }
    String[] parts = s.split(",");
    List<String> out = new ArrayList<>();
    for (String p : parts) {
      String id = p.trim();
      if (!id.isBlank()) {
        out.add(id);
      }
    }
    return out;
  }
}
