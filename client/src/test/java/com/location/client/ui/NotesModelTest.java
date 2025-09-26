package com.location.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.location.client.core.MockDataSource;
import com.location.client.core.Models;
import org.junit.jupiter.api.Test;

class NotesModelTest {

  @Test
  void notes_are_preserved_when_updating_in_mock() {
    MockDataSource ds = new MockDataSource();
    PlanningPanel panel = new PlanningPanel(ds);
    panel.reload();
    Models.Intervention first = panel.getInterventions().get(0);
    Models.Intervention withNotes =
        new Models.Intervention(
            first.id(),
            first.agencyId(),
            first.resourceId(),
            first.clientId(),
            first.driverId(),
            first.title(),
            first.start(),
            first.end(),
            "HELLO");
    Models.Intervention saved = ds.updateIntervention(withNotes);
    assertEquals("HELLO", saved.notes());
  }
}
