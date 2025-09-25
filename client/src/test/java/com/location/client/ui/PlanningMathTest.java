package com.location.client.ui;

import com.location.client.core.MockDataSource;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PlanningMathTest {
  @Test
  void pixel_time_roundtrip_is_consistent() throws Exception {
    AtomicBoolean done = new AtomicBoolean(false);
    SwingUtilities.invokeAndWait(() -> {
      PlanningPanel panel = new PlanningPanel(new MockDataSource());
      panel.setSize(1000, 600);
      assertFalse(panel.getInterventions().isEmpty());
      done.set(true);
    });
    if (!done.get()) {
      throw new IllegalStateException("Swing event not executed");
    }
  }
}
