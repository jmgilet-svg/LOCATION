package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TemplatesEditorFrame extends JFrame {

  private final DataSourceProvider dsp;
  private final DefaultListModel<Models.Template> listModel = new DefaultListModel<>();
  private final JList<Models.Template> list = new JList<>(listModel);
  private final JTextField keyField = new JTextField();
  private final javax.swing.JComboBox<Models.TemplateKind> kindCombo =
      new javax.swing.JComboBox<>(Models.TemplateKind.values());
  private final JTextArea editor = new JTextArea();
  private final JEditorPane preview = new JEditorPane("text/html", "");
  private final Timer previewTimer = new Timer(300, e -> doRender(false));
  private final JCheckBox cbAuto = new JCheckBox("Preview auto", true);
  private final JPopupMenu varMenu = new JPopupMenu();

  private Models.Template currentTemplate = null;
  private String currentKey = null;
  private boolean updating = false;

  public TemplatesEditorFrame(DataSourceProvider dsp) {
    super("Templates Documents & Emails");
    this.dsp = dsp;
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(6, 6));

    JPanel left = new JPanel(new BorderLayout(6, 6));
    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
    top.add(new JLabel("Type:"));
    kindCombo.setPreferredSize(new Dimension(140, kindCombo.getPreferredSize().height));
    top.add(kindCombo);
    top.add(new JLabel("Clé:"));
    keyField.setColumns(14);
    top.add(keyField);
    JButton save = new JButton("Enregistrer");
    JButton render = new JButton("Prévisualiser");
    JButton sendTest = new JButton("Envoyer test...");
    JButton insertVar = new JButton("Insérer variable");
    JButton btNew = new JButton("Nouveau");
    JButton btDelete = new JButton("Supprimer");
    top.add(save);
    top.add(render);
    top.add(sendTest);
    top.add(insertVar);
    top.add(cbAuto);
    left.add(top, BorderLayout.NORTH);

    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(createRenderer());
    list.addListSelectionListener(
        e -> {
          if (!e.getValueIsAdjusting()) {
            loadSelected();
          }
        });
    left.add(new JScrollPane(list), BorderLayout.CENTER);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
    bottom.add(btNew);
    bottom.add(btDelete);
    left.add(bottom, BorderLayout.SOUTH);

    editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    editor.setLineWrap(false);
    JScrollPane editorScroll = new JScrollPane(editor);

    preview.setEditable(false);
    preview.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    JScrollPane previewScroll = new JScrollPane(preview);

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorScroll, previewScroll);
    split.setResizeWeight(0.5);
    split.setContinuousLayout(true);

    add(split, BorderLayout.CENTER);
    add(left, BorderLayout.WEST);

    previewTimer.setRepeats(false);
    buildVarMenu();
    editor.getDocument().addDocumentListener(new PreviewListener());

    // Actions
    save.addActionListener(e -> doSave());
    render.addActionListener(e -> doRender(true));
    sendTest.addActionListener(e -> doSendTest());
    insertVar.addActionListener(e -> showVarMenu(insertVar));
    btNew.addActionListener(e -> newTemplate());
    btDelete.addActionListener(e -> deleteSelected());
    cbAuto.addActionListener(
        e -> {
          if (cbAuto.isSelected()) {
            previewTimer.restart();
          }
        });

    setSize(1100, 700);
    setLocationRelativeTo(null);

    refreshList(null);
    if (listModel.isEmpty()) {
      newTemplate();
    }
  }

  private ListCellRenderer<? super Models.Template> createRenderer() {
    return (list, value, index, isSelected, cellHasFocus) -> {
      JLabel label = new JLabel();
      label.setOpaque(true);
      if (value != null) {
        label.setText(value.kind().name() + " · " + value.key());
      } else {
        label.setText("« aucun »");
      }
      if (isSelected) {
        label.setBackground(list.getSelectionBackground());
        label.setForeground(list.getSelectionForeground());
      } else {
        label.setBackground(list.getBackground());
        label.setForeground(list.getForeground());
      }
      label.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 6));
      return label;
    };
  }

  private void refreshList(String selectId) {
    List<Models.Template> templates;
    try {
      templates = dsp.listTemplates();
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(
          this,
          "Impossible de charger les templates : " + ex.getMessage(),
          "Erreur",
          JOptionPane.ERROR_MESSAGE);
      templates = List.of();
    }
    templates =
        templates.stream()
            .sorted(
                java.util.Comparator.comparing(Models.Template::kind, java.util.Comparator.comparing(Enum::name))
                    .thenComparing(Models.Template::key))
            .toList();
    updating = true;
    listModel.clear();
    for (Models.Template template : templates) {
      listModel.addElement(template);
    }
    updating = false;
    if (selectId != null) {
      selectById(selectId);
    } else if (!listModel.isEmpty()) {
      list.setSelectedIndex(0);
    }
  }

  private void selectById(String id) {
    if (id == null) {
      return;
    }
    for (int i = 0; i < listModel.size(); i++) {
      Models.Template template = listModel.get(i);
      if (Objects.equals(template.id(), id)) {
        list.setSelectedIndex(i);
        list.ensureIndexIsVisible(i);
        return;
      }
    }
    list.clearSelection();
  }

  private void loadSelected() {
    if (updating) {
      return;
    }
    Models.Template selected = list.getSelectedValue();
    updating = true;
    try {
      currentTemplate = selected;
      if (selected == null) {
        currentKey = null;
        keyField.setText("");
        kindCombo.setSelectedIndex(0);
        editor.setText("");
        preview.setText("");
      } else {
        currentKey = selected.key();
        keyField.setText(selected.key());
        kindCombo.setSelectedItem(selected.kind());
        editor.setText(selected.html());
        editor.setCaretPosition(0);
        if (cbAuto.isSelected()) {
          previewTimer.restart();
        } else {
          doRender(false);
        }
      }
    } finally {
      updating = false;
    }
  }

  private void doSave() {
    String key = keyField.getText().trim();
    if (key.isBlank()) {
      JOptionPane.showMessageDialog(
          this, "La clé du template est requise.", "Templates", JOptionPane.WARNING_MESSAGE);
      keyField.requestFocusInWindow();
      return;
    }
    Models.TemplateKind kind = (Models.TemplateKind) kindCombo.getSelectedItem();
    if (kind == null) {
      kind = Models.TemplateKind.EMAIL;
    }
    Models.Template payload =
        new Models.Template(
            currentTemplate == null ? null : currentTemplate.id(), key, kind, editor.getText());
    try {
      Models.Template saved = dsp.saveTemplate(payload);
      currentTemplate = saved;
      currentKey = saved.key();
      refreshList(saved.id());
      JOptionPane.showMessageDialog(
          this, "Template enregistré.", "Templates", JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(
          this, "Erreur lors de l'enregistrement : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void doRender(boolean manual) {
    if (updating) {
      return;
    }
    String html = editor.getText();
    if (html == null) {
      html = "";
    }
    String merged = applySampleContext(html);
    preview.setText(merged);
    preview.setCaretPosition(0);
  }

  private String applySampleContext(String template) {
    if (template == null || template.isEmpty()) {
      return "";
    }
    Map<String, String> context = new LinkedHashMap<>();
    context.put("client.name", "Jean Dupont");
    context.put("client.email", "jean.dupont@example.com");
    context.put("intervention.title", "Levage chantier Alpha");
    context.put("intervention.start", "2025-03-15 08:00");
    context.put("intervention.end", "2025-03-15 12:00");
    context.put("agency.name", "Agence Démo");
    context.put("doc.reference", "DV-2025-0001");
    context.put("doc.totalTtc", "1 234,00 €");

    String rendered = template;
    for (Map.Entry<String, String> entry : context.entrySet()) {
      rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return rendered;
  }

  private void doSendTest() {
    String to =
        JOptionPane.showInputDialog(this, "Destinataire", "Envoyer test", JOptionPane.QUESTION_MESSAGE);
    if (to == null || to.isBlank()) {
      return;
    }
    String subject =
        JOptionPane.showInputDialog(this, "Sujet", currentKey == null ? "" : currentKey);
    if (subject == null) {
      return;
    }
    try {
      dsp.sendEmail(to, subject, editor.getText());
      JOptionPane.showMessageDialog(
          this, "Email (test) envoyé (stub).", "Email", JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(
          this, "Envoi impossible : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void newTemplate() {
    list.clearSelection();
    currentTemplate = null;
    currentKey = null;
    keyField.setText("");
    kindCombo.setSelectedIndex(0);
    editor.setText("");
    preview.setText("");
    keyField.requestFocusInWindow();
  }

  private void deleteSelected() {
    Models.Template selected = list.getSelectedValue();
    if (selected == null || selected.id() == null || selected.id().isBlank()) {
      return;
    }
    int confirm =
        JOptionPane.showConfirmDialog(
            this,
            "Supprimer le template " + selected.key() + " ?",
            "Supprimer",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
    if (confirm != JOptionPane.OK_OPTION) {
      return;
    }
    try {
      dsp.deleteTemplate(selected.id());
      currentTemplate = null;
      currentKey = null;
      refreshList(null);
      newTemplate();
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(
          this, "Suppression impossible : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void buildVarMenu() {
    String[] vars = {
      "client.name",
      "client.email",
      "intervention.title",
      "intervention.start",
      "intervention.end",
      "agency.name"
    };
    for (String v : vars) {
      javax.swing.JMenuItem it = new javax.swing.JMenuItem("{{" + v + "}}");
      it.addActionListener(e -> insertAtCaret("{{" + v + "}}"));
      varMenu.add(it);
    }
  }

  private void showVarMenu(JComponent invoker) {
    varMenu.show(invoker, 0, invoker.getHeight());
  }

  private void insertAtCaret(String text) {
    try {
      int pos = editor.getCaretPosition();
      editor.getDocument().insertString(pos, text, null);
      editor.requestFocusInWindow();
    } catch (Exception ignore) {
    }
  }

  private class PreviewListener implements DocumentListener {
    private void update() {
      if (updating) {
        return;
      }
      if (cbAuto.isSelected()) {
        previewTimer.restart();
      }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      update();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      update();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      update();
    }
  }
}
