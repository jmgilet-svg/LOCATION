package com.location.client.ui;

import com.location.client.core.MockDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SelectionTest {
  @Test
  void selection_is_null_by_default() {
    PlanningPanel panel = new PlanningPanel(new MockDataSource());
    assertNull(panel.getSelected());
    assertNull(panel.getSelectedInterventionId());
  }
}
