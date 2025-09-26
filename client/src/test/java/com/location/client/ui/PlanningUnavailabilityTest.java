package com.location.client.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.location.client.core.MockDataSource;
import org.junit.jupiter.api.Test;

class PlanningUnavailabilityTest {

  @Test
  void mockContainsUnavailabilities() {
    MockDataSource mock = new MockDataSource();
    PlanningPanel panel = new PlanningPanel(mock);
    panel.setSize(800, 600);
    assertFalse(panel.getUnavailabilities().isEmpty());
  }
}
