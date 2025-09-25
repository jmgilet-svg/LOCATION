package com.location.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.location.client.core.Models;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanningModelTest {
  @Test
  void overlapDetectionAndMoveSnap() {
    PlanningModel pm = new PlanningModel();
    pm.setSnapMinutes(30);
    Instant t0 = Instant.parse("2025-01-01T08:00:00Z");
    Models.Intervention a =
        new Models.Intervention("A", "Ag", "R1", "C", "T", t0, t0.plus(Duration.ofHours(2)));
    Models.Intervention b =
        new Models.Intervention(
            "B",
            "Ag",
            "R1",
            "C",
            "T2",
            t0.plus(Duration.ofHours(1)),
            t0.plus(Duration.ofHours(3)));
    assertTrue(pm.overlaps(a, b));
    assertTrue(pm.hasConflict(b, List.of(a, b), "B"));
    Models.Intervention moved = pm.move(a, Duration.ofMinutes(40));
    assertEquals(Instant.parse("2025-01-01T08:30:00Z"), moved.start());
    assertEquals(Duration.ofHours(2), Duration.between(moved.start(), moved.end()));
  }
}
