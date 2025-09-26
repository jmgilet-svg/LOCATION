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

  Models.Intervention updateIntervention(Models.Intervention intervention);

  void deleteIntervention(String id);

  List<Models.Unavailability> listUnavailabilities(
      java.time.OffsetDateTime from, java.time.OffsetDateTime to, String resourceId);

  Models.Unavailability createUnavailability(Models.Unavailability unavailability);

  List<Models.RecurringUnavailability> listRecurringUnavailabilities(String resourceId);

  Models.RecurringUnavailability createRecurringUnavailability(Models.RecurringUnavailability recurring);

  java.nio.file.Path downloadResourcesCsv(String tags, java.nio.file.Path target);

  java.nio.file.Path downloadInterventionPdf(String interventionId, java.nio.file.Path target);

  void emailInterventionPdf(String interventionId, String to, String subject, String message);

  java.nio.file.Path downloadClientsCsv(java.nio.file.Path target);

  java.nio.file.Path downloadUnavailabilitiesCsv(
      java.time.OffsetDateTime from,
      java.time.OffsetDateTime to,
      String resourceId,
      java.nio.file.Path target);

  java.util.Map<String, Boolean> getServerFeatures();

}
