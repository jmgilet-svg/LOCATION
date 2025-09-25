package com.location.client.ui;

import com.location.client.core.Models;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class PlanningModel {
  private int snapMinutes = 15;

  public int getSnapMinutes() {
    return snapMinutes;
  }

  public void setSnapMinutes(int minutes) {
    if (minutes <= 0) {
      throw new IllegalArgumentException("snapMinutes must be positive");
    }
    this.snapMinutes = minutes;
  }

  public boolean overlaps(Models.Intervention a, Models.Intervention b) {
    Objects.requireNonNull(a, "a");
    Objects.requireNonNull(b, "b");
    return a.start().isBefore(b.end()) && a.end().isAfter(b.start());
  }

  public boolean hasConflict(Models.Intervention candidate, List<Models.Intervention> interventions, String ignoreId) {
    Objects.requireNonNull(candidate, "candidate");
    if (interventions == null || interventions.isEmpty()) {
      return false;
    }
    for (Models.Intervention other : interventions) {
      if (other == null) {
        continue;
      }
      if (ignoreId != null && ignoreId.equals(other.id())) {
        continue;
      }
      if (candidate.id() != null && candidate.id().equals(other.id())) {
        continue;
      }
      if (!Objects.equals(candidate.resourceId(), other.resourceId())) {
        continue;
      }
      if (overlaps(candidate, other)) {
        return true;
      }
    }
    return false;
  }

  public Models.Intervention move(Models.Intervention intervention, Duration delta) {
    Objects.requireNonNull(intervention, "intervention");
    Objects.requireNonNull(delta, "delta");
    Instant newStart = intervention.start().plus(delta);
    Instant snapped = snapToGrid(newStart);
    Duration originalDuration = Duration.between(intervention.start(), intervention.end());
    Instant newEnd = snapped.plus(originalDuration);
    return new Models.Intervention(
        intervention.id(),
        intervention.agencyId(),
        intervention.resourceId(),
        intervention.clientId(),
        intervention.title(),
        snapped,
        newEnd);
  }

  private Instant snapToGrid(Instant instant) {
    long unitSeconds = snapMinutes * 60L;
    long seconds = ChronoUnit.SECONDS.between(Instant.EPOCH, instant);
    double ratio = seconds / (double) unitSeconds;
    long snappedUnits = Math.round(ratio);
    long snappedSeconds = snappedUnits * unitSeconds;
    return Instant.EPOCH.plusSeconds(snappedSeconds);
  }
}
