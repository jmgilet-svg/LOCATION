package com.location.client.ui;

import java.util.ArrayDeque;
import java.util.Deque;

/** Pile d'historique Undo/Redo minimaliste, à utiliser sur l'EDT. */
public final class History {
  public static final class Entry {
    private final String label;
    private final Runnable undo;
    private final Runnable redo;

    private Entry(String label, Runnable undo, Runnable redo) {
      this.label = label;
      this.undo = undo;
      this.redo = redo;
    }

    private void doUndo() {
      if (undo != null) {
        undo.run();
      }
    }

    private void doRedo() {
      if (redo != null) {
        redo.run();
      }
    }
  }

  private final Deque<Entry> undos = new ArrayDeque<>();
  private final Deque<Entry> redos = new ArrayDeque<>();

  private History() {}

  public static History create() {
    return new History();
  }

  public void push(String label, Runnable undo, Runnable redo) {
    undos.push(new Entry(label, undo, redo));
    redos.clear();
  }

  public boolean canUndo() {
    return !undos.isEmpty();
  }

  public boolean canRedo() {
    return !redos.isEmpty();
  }

  public String undo() {
    if (undos.isEmpty()) {
      return null;
    }
    Entry entry = undos.pop();
    entry.doUndo();
    redos.push(entry);
    return entry.label;
  }

  public String redo() {
    if (redos.isEmpty()) {
      return null;
    }
    Entry entry = redos.pop();
    entry.doRedo();
    undos.push(entry);
    return entry.label;
  }
}
