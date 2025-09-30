package com.location.client.ui.uikit;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/** Small helpers to make tables feel modern: sorter + fuzzy search with debounce. */
public final class TableUtils {
  private TableUtils() {}

  public static void enableSorting(JTable table) {
    if (!(table.getRowSorter() instanceof TableRowSorter)) {
      TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
      table.setRowSorter(sorter);
    }
  }

  public static void applySearch(JTable table, JTextField searchField, int debounceMs) {
    enableSorting(table);
    final TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
    final Timer timer = new Timer(debounceMs, e -> {
      String q = searchField.getText().trim().toLowerCase();
      if (q.isEmpty()) {
        sorter.setRowFilter(null);
        return;
      }
      sorter.setRowFilter(
          new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
              for (int c = 0; c < entry.getValueCount(); c++) {
                Object v = entry.getValue(c);
                if (v != null) {
                  String s = v.toString().toLowerCase();
                  if (s.contains(q) || fuzzy(s, q)) {
                    return true;
                  }
                }
              }
              return false;
            }
          });
    });
    timer.setRepeats(false);
    searchField
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              private void schedule() {
                timer.restart();
              }

              @Override
              public void insertUpdate(DocumentEvent e) {
                schedule();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                schedule();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                schedule();
              }
            });
  }

  // super simple fuzzy contains: all chars of needle appear in order in hay
  private static boolean fuzzy(String hay, String needle) {
    int j = 0;
    for (int i = 0; i < hay.length() && j < needle.length(); i++) {
      if (hay.charAt(i) == needle.charAt(j)) {
        j++;
      }
    }
    return j == needle.length();
  }
}
