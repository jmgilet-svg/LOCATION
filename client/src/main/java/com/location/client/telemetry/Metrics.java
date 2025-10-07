package com.location.client.telemetry;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Lightweight in-memory metrics aggregator for the client runtime.
 */
public final class Metrics {
  private static final Metrics INSTANCE = new Metrics();
  private static final int MAX_EVENTS = 400;

  public static Metrics get() {
    return INSTANCE;
  }

  private final ConcurrentMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Stats> histograms = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<Consumer<Event>> listeners = new CopyOnWriteArrayList<>();
  private final Deque<Event> events = new ArrayDeque<>();

  private Metrics() {}

  public long getCounter(String key) {
    if (key == null) {
      return 0L;
    }
    AtomicLong counter = counters.get(key);
    return counter != null ? counter.get() : 0L;
  }

  public void setCounter(String key, long value) {
    if (key == null) {
      return;
    }
    counters.compute(key, (k, existing) -> {
      if (existing == null) {
        existing = new AtomicLong();
      }
      existing.set(value);
      return existing;
    });
  }

  public void increment(String key) {
    increment(key, 1L);
  }

  public void increment(String key, long delta) {
    if (key == null) {
      return;
    }
    counters.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(delta);
  }

  public void observe(String key, long value) {
    if (key == null) {
      return;
    }
    Stats stats = histograms.computeIfAbsent(key, k -> new Stats());
    stats.total.addAndGet(value);
    stats.count.incrementAndGet();
  }

  public double avgMs(String key) {
    if (key == null) {
      return 0d;
    }
    Stats stats = histograms.get(key);
    if (stats == null) {
      return 0d;
    }
    long count = stats.count.get();
    if (count <= 0L) {
      return 0d;
    }
    return stats.total.get() / (double) count;
  }

  public void event(String type, String... attributes) {
    if (type == null || type.isBlank()) {
      return;
    }
    Map<String, String> payload = new java.util.LinkedHashMap<>();
    if (attributes != null) {
      for (int i = 0; i + 1 < attributes.length; i += 2) {
        String key = Objects.toString(attributes[i], "");
        String value = Objects.toString(attributes[i + 1], "");
        payload.put(key, value);
      }
    }
    Event event = new Event(Instant.now(), type, java.util.Map.copyOf(payload));
    synchronized (events) {
      events.addLast(event);
      while (events.size() > MAX_EVENTS) {
        events.removeFirst();
      }
    }
    for (Consumer<Event> listener : listeners) {
      try {
        listener.accept(event);
      } catch (RuntimeException ignored) {
      }
    }
  }

  public List<Event> recentEvents() {
    synchronized (events) {
      return new ArrayList<>(events);
    }
  }

  public void clearEvents() {
    synchronized (events) {
      events.clear();
    }
  }

  public void addEventListener(Consumer<Event> listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  public void removeEventListener(Consumer<Event> listener) {
    if (listener != null) {
      listeners.remove(listener);
    }
  }

  private static final class Stats {
    final AtomicLong total = new AtomicLong();
    final AtomicLong count = new AtomicLong();
  }

  public static final class Event {
    private final Instant timestamp;
    private final String type;
    private final Map<String, String> attributes;

    Event(Instant timestamp, String type, Map<String, String> attributes) {
      this.timestamp = timestamp;
      this.type = type;
      this.attributes = attributes;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public String getType() {
      return type;
    }

    public Map<String, String> getAttributes() {
      return attributes;
    }
  }
}
