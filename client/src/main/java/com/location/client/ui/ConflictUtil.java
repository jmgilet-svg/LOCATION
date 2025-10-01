package com.location.client.ui;

import com.location.client.core.Models;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ConflictUtil {
  private ConflictUtil() {}

  public record Conflict(Models.Intervention a, Models.Intervention b, String resourceId) {}

  public static List<Conflict> computeConflicts(List<Models.Intervention> interventions) {
    List<Conflict> out = new ArrayList<>();
    if (interventions == null || interventions.isEmpty()) {
      return out;
    }
    for (int i = 0; i < interventions.size(); i++) {
      Models.Intervention a = interventions.get(i);
      for (int j = i + 1; j < interventions.size(); j++) {
        Models.Intervention b = interventions.get(j);
        if (!overlap(a.start(), a.end(), b.start(), b.end())) {
          continue;
        }
        if (a.resourceIds() == null || b.resourceIds() == null) {
          continue;
        }
        for (String resourceId : a.resourceIds()) {
          if (resourceId != null && b.resourceIds().contains(resourceId)) {
            out.add(new Conflict(a, b, resourceId));
          }
        }
      }
    }
    return out;
  }

  private static boolean overlap(Instant startA, Instant endA, Instant startB, Instant endB) {
    if (startA == null || endA == null || startB == null || endB == null) {
      return false;
    }
    return startA.isBefore(endB) && endA.isAfter(startB);
  }
}
