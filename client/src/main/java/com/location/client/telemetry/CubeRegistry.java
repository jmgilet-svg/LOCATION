package com.location.client.telemetry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * D2 — Petit registre en mémoire pour relier une clé de conflit à ses métadonnées.
 */
public final class CubeRegistry {
  private static final CubeRegistry INSTANCE = new CubeRegistry();

  public static CubeRegistry get() {
    return INSTANCE;
  }

  private final Map<String, Meta> metas = new ConcurrentHashMap<>();

  private CubeRegistry() {}

  public void put(Meta meta) {
    if (meta == null || meta.key() == null || meta.key().isBlank()) {
      return;
    }
    metas.put(meta.key(), meta);
  }

  public Meta get(String key) {
    if (key == null) {
      return null;
    }
    return metas.get(key);
  }

  public void remove(String key) {
    if (key == null) {
      return;
    }
    metas.remove(key);
  }

  public static record Meta(
      String key, String resourceId, String agencyId, String resourceTypeId, List<String> clients, int severity) {}
}
