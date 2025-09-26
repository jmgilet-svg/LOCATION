package com.location.client.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/** Éditeur HTML simple avec toolbar + preview live. */
public class HtmlEditorPanel extends JPanel {
  private final JTextArea editor = new JTextArea();
  private final JEditorPane preview = new JEditorPane();
  private Consumer<String> onChange;

  public HtmlEditorPanel() {
    super(new BorderLayout());
    JToolBar tb = new JToolBar();
    tb.setFloatable(false);
    tb.add(new JButton(new AbstractAction("Gras") {
      @Override
      public void actionPerformed(ActionEvent e) {
        wrap("<b>", "</b>");
      }
    }));
    tb.add(new JButton(new AbstractAction("Italique") {
      @Override
      public void actionPerformed(ActionEvent e) {
        wrap("<i>", "</i>");
      }
    }));
    tb.addSeparator();
    tb.add(new JButton(new AbstractAction("H1") {
      @Override
      public void actionPerformed(ActionEvent e) {
        wrap("<h1>", "</h1>");
      }
    }));
    tb.add(new JButton(new AbstractAction("H2") {
      @Override
      public void actionPerformed(ActionEvent e) {
        wrap("<h2>", "</h2>");
      }
    }));
    tb.addSeparator();
    tb.add(new JButton(new AbstractAction("• Liste") {
      @Override
      public void actionPerformed(ActionEvent e) {
        list(false);
      }
    }));
    tb.add(new JButton(new AbstractAction("1. Liste") {
      @Override
      public void actionPerformed(ActionEvent e) {
        list(true);
      }
    }));
    tb.addSeparator();
    JComboBox<String> vars = new JComboBox<>(new String[]{"${clientName}", "${docNumber}", "${docDate}", "${totalHT}", "${totalTTC}", "${agencyName}"});
    JButton ins = new JButton("Insérer variable");
    ins.addActionListener(e -> insert((String) vars.getSelectedItem()));
    tb.add(vars);
    tb.add(ins);
    add(tb, BorderLayout.NORTH);

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      @Override
      public void insertUpdate(javax.swing.event.DocumentEvent e) {
        updatePreview();
      }

      @Override
      public void removeUpdate(javax.swing.event.DocumentEvent e) {
        updatePreview();
      }

      @Override
      public void changedUpdate(javax.swing.event.DocumentEvent e) {
        updatePreview();
      }
    });
    split.setLeftComponent(new JScrollPane(editor));
    preview.setEditable(false);
    preview.setContentType("text/html");
    split.setRightComponent(new JScrollPane(preview));
    split.setResizeWeight(0.55);
    add(split, BorderLayout.CENTER);
  }

  private void updatePreview() {
    preview.setText(editor.getText());
    if (onChange != null) {
      onChange.accept(editor.getText());
    }
  }

  public void setOnChange(Consumer<String> c) {
    this.onChange = c;
  }

  public void setHtml(String html) {
    editor.setText(html == null ? "" : html);
    updatePreview();
  }

  public String getHtml() {
    return editor.getText();
  }

  private void wrap(String open, String close) {
    try {
      int start = editor.getSelectionStart();
      int end = editor.getSelectionEnd();
      if (start == end) {
        editor.getDocument().insertString(start, open + close, null);
        editor.setCaretPosition(start + open.length());
      } else {
        String sel = editor.getSelectedText();
        editor.getDocument().remove(start, end - start);
        editor.getDocument().insertString(start, open + sel + close, null);
      }
    } catch (BadLocationException ignored) {
    }
  }

  private void list(boolean ordered) {
    try {
      int startLine = editor.getLineOfOffset(editor.getSelectionStart());
      int endLine = editor.getLineOfOffset(editor.getSelectionEnd());
      StringBuilder sb = new StringBuilder(ordered ? "<ol>\n" : "<ul>\n");
      for (int line = startLine; line <= endLine; line++) {
        int s = editor.getLineStartOffset(line);
        int e = editor.getLineEndOffset(line);
        String text = editor.getText(s, e - s).trim();
        if (!text.isEmpty()) {
          sb.append("<li>").append(text).append("</li>\n");
        }
      }
      sb.append(ordered ? "</ol>\n" : "</ul>\n");
      editor.replaceRange(sb.toString(), editor.getSelectionStart(), editor.getSelectionEnd());
    } catch (Exception ignored) {
    }
  }

  private void insert(String s) {
    try {
      int pos = editor.getCaretPosition();
      editor.getDocument().insertString(pos, s, null);
    } catch (BadLocationException ignored) {
    }
  }
}
