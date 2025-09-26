package com.location.client.core;

import java.util.List;

public interface DataSourceProvider extends AutoCloseable {
  String getLabel(); // "MOCK" or "REST"

  void resetDemoData(); // no-op for REST

  List<Models.Agency> listAgencies();

  List<Models.Client> listClients();

  List<Models.Resource> listResources();

  List<Models.Intervention> listInterventions(
      java.time.OffsetDateTime from, java.time.OffsetDateTime to, String resourceId);

  Models.Intervention createIntervention(Models.Intervention intervention);

  List<Models.Unavailability> listUnavailabilities(
      java.time.OffsetDateTime from, java.time.OffsetDateTime to, String resourceId);

  Models.Unavailability createUnavailability(Models.Unavailability unavailability);
}
