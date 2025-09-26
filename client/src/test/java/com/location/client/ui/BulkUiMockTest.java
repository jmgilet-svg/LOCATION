package com.location.client.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.location.client.core.MockDataSource;
import org.junit.jupiter.api.Test;

class BulkUiMockTest {

  @Test
  void mockBulkDoesNotThrow() {
    MockDataSource dataSource = new MockDataSource();
    PlanningPanel panel = new PlanningPanel(dataSource);
    panel.reload();
    var ids = panel.getInterventions().stream().map(i -> i.id()).toList();
    assertDoesNotThrow(() -> dataSource.emailBulk(ids, null));
  }
}
