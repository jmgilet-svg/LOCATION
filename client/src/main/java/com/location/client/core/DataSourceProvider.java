package com.location.client.core;

import java.util.List;

public interface DataSourceProvider extends AutoCloseable {
  String getLabel(); // "MOCK" or "REST"

  void resetDemoData(); // no-op for REST (legacy)

  default void resetDemo() {
    resetDemoData();
  }

  List<Models.Agency> listAgencies();

  List<Models.Client> listClients();

  String getCurrentAgencyId();

  void setCurrentAgencyId(String agencyId);

  List<Models.Resource> listResources();

  default List<Models.ResourceType> listResourceTypes() {
    return java.util.List.of();
  }

  default Models.ResourceType saveResourceType(Models.ResourceType resourceType) {
    throw new UnsupportedOperationException("saveResourceType non disponible dans " + getLabel());
  }

  default void deleteResourceType(String id) {
    throw new UnsupportedOperationException("deleteResourceType non disponible dans " + getLabel());
  }

  default String getResourceTypeForResource(String resourceId) {
    return null;
  }

  default void setResourceTypeForResource(String resourceId, String resourceTypeId) {
    throw new UnsupportedOperationException("setResourceTypeForResource non disponible dans " + getLabel());
  }

  default double getResourceDailyRate(String resourceId) {
    return 0.0;
  }

  default void setResourceDailyRate(String resourceId, double rate) {
    throw new UnsupportedOperationException("setResourceDailyRate non disponible dans " + getLabel());
  }

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

  java.nio.file.Path downloadInterventionsCsv(
      java.time.OffsetDateTime from, java.time.OffsetDateTime to, java.nio.file.Path target);

  java.nio.file.Path downloadClientsCsv(java.nio.file.Path target);

  java.nio.file.Path downloadUnavailabilitiesCsv(
      java.time.OffsetDateTime from,
      java.time.OffsetDateTime to,
      String resourceId,
      java.nio.file.Path target);

  java.util.Map<String, Boolean> getServerFeatures();

  java.util.List<Models.Doc> listDocs(String type, String clientId);

  Models.Doc createDoc(String type, String agencyId, String clientId, String title);

  Models.Doc updateDoc(Models.Doc document);

  void deleteDoc(String id);

  Models.Doc transitionDoc(String id, String toType);

  java.nio.file.Path downloadDocPdf(String id, java.nio.file.Path target);

  void emailDoc(String id, String to, String subject, String message);

  java.nio.file.Path downloadDocsCsv(String type, String clientId, java.nio.file.Path target);

  Models.EmailTemplate getEmailTemplate(String docType);

  Models.EmailTemplate saveEmailTemplate(String docType, String subject, String body);

  Models.DocTemplate getDocTemplate(String docType);

  Models.DocTemplate saveDocTemplate(String docType, String html);

  void emailDocsBatch(java.util.List<String> ids, String to, String subject, String message);

  default Models.EmailTemplate getAgencyEmailTemplate(String agencyId) {
    return getAgencyEmailTemplate(agencyId, normalizeTemplateKey(null));
  }

  Models.EmailTemplate getAgencyEmailTemplate(String agencyId, String templateKey);

  default Models.EmailTemplate updateAgencyEmailTemplate(
      String agencyId, Models.EmailTemplate template) {
    if (template == null) {
      throw new IllegalArgumentException("Template e-mail requis");
    }
    String key = normalizeTemplateKey(template.key());
    return updateAgencyEmailTemplate(agencyId, key, template.subject(), template.html());
  }

  Models.EmailTemplate updateAgencyEmailTemplate(
      String agencyId, String templateKey, String subject, String html);

  default void emailBulk(java.util.List<String> ids, String toOverride) {
    throw new UnsupportedOperationException("emailBulk(ids,to) non disponible dans " + getLabel());
  }

  void emailBulk(java.util.List<String> recipients, String subject, String html);

  default Models.Resource saveResource(Models.Resource resource) {
    throw new UnsupportedOperationException("saveResource non disponible dans " + getLabel());
  }

  default java.util.List<Models.Unavailability> listUnavailability(String resourceId) {
    throw new UnsupportedOperationException("listUnavailability non disponible dans " + getLabel());
  }

  default Models.Unavailability saveUnavailability(Models.Unavailability unavailability) {
    throw new UnsupportedOperationException("saveUnavailability non disponible dans " + getLabel());
  }

  default void deleteUnavailability(String id) {
    throw new UnsupportedOperationException("deleteUnavailability non disponible dans " + getLabel());
  }

  default java.util.List<Models.RecurringUnavailability> listRecurringUnavailability(String resourceId) {
    return listRecurringUnavailabilities(resourceId);
  }

  default Models.RecurringUnavailability saveRecurringUnavailability(
      Models.RecurringUnavailability recurring) {
    return createRecurringUnavailability(recurring);
  }

  default void deleteRecurringUnavailability(String id) {
    throw new UnsupportedOperationException(
        "deleteRecurringUnavailability non disponible dans " + getLabel());
  }

  static String normalizeTemplateKey(String templateKey) {
    return (templateKey == null || templateKey.isBlank()) ? "default" : templateKey;
  }
}
