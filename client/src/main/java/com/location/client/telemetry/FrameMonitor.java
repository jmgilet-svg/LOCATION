package com.location.client.telemetry;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import javax.swing.Timer;

/** D3 — Surveille l'EDT : frame-time p95 et freezes (lag EDT > seuil). */
public final class FrameMonitor {
  private static final FrameMonitor INSTANCE = new FrameMonitor();

  public static FrameMonitor get() {
    return INSTANCE;
  }

  private final Metrics metrics = Metrics.get();
  private final Timer heartbeat;
  private long lastBeat = System.nanoTime();
  private final Deque<Double> framesMs = new ArrayDeque<>();
  private int freezes = 0;
  private final int window = 200;
  private final double freezeThresholdMs = 120.0;

  private FrameMonitor() {
    heartbeat = new Timer(50, e -> beat());
    heartbeat.setRepeats(true);
  }

  public void start() {
    if (!heartbeat.isRunning()) {
      lastBeat = System.nanoTime();
      heartbeat.start();
    }
  }

  public void stop() {
    if (heartbeat.isRunning()) {
      heartbeat.stop();
    }
  }

  private void beat() {
    long now = System.nanoTime();
    double dtMs = (now - lastBeat) / 1_000_000.0;
    lastBeat = now;
    if (dtMs > freezeThresholdMs) {
      freezes++;
      metrics.event("ux.freeze", "dt.ms", String.format(java.util.Locale.ROOT, "%.1f", dtMs));
    }
  }

  public void observeFrame(double ms) {
    framesMs.addLast(ms);
    while (framesMs.size() > window) {
      framesMs.removeFirst();
    }
  }

  public double p95FrameMs() {
    if (framesMs.isEmpty()) {
      return 0.0;
    }
    Double[] values = framesMs.toArray(new Double[0]);
    Arrays.sort(values);
    int index = (int) Math.round((values.length - 1) * 0.95);
    index = Math.max(0, Math.min(values.length - 1, index));
    return values[index];
  }

  public int freezeCount() {
    return freezes;
  }

  public int windowSize() {
    return window;
  }

  public int freezeThresholdMs() {
    return (int) freezeThresholdMs;
  }
}
