package com.location.client.core;

import java.util.List;

public interface DataSourceProvider extends AutoCloseable {
  String getLabel(); // "MOCK" or "REST"

  void resetDemoData();

  default void generateCurrentMonthDemo() {}

  default void resetDemo() {
    resetDemoData();
  }

  List<Models.Agency> listAgencies();

  default Models.Agency getAgency(String id) {
    if (id == null || id.isBlank()) {
      return null;
    }
    return listAgencies().stream().filter(agency -> id.equals(agency.id())).findFirst().orElse(null);
  }

  default Models.Agency saveAgency(Models.Agency agency) {
    throw new UnsupportedOperationException("saveAgency non disponible dans " + getLabel());
  }

  List<Models.Client> listClients();

  default Models.Client saveClient(Models.Client client) {
    throw new UnsupportedOperationException("saveClient non disponible dans " + getLabel());
  }

  default void deleteClient(String clientId) {
    throw new UnsupportedOperationException("deleteClient non disponible dans " + getLabel());
  }

  default List<Models.Contact> listContacts(String clientId) {
    return java.util.List.of();
  }

  default Models.Contact saveContact(Models.Contact contact) {
    throw new UnsupportedOperationException("saveContact non disponible dans " + getLabel());
  }

  default void deleteContact(String contactId) {
    throw new UnsupportedOperationException("deleteContact non disponible dans " + getLabel());
  }

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

  default void emailDoc(String id, String to, String subject, String message) {
    emailDoc(id, to, subject, message, true);
  }

  void emailDoc(String id, String to, String subject, String message, boolean attachPdf);

  java.nio.file.Path downloadDocsCsv(String type, String clientId, java.nio.file.Path target);

  Models.EmailTemplate getEmailTemplate(String docType);

  Models.EmailTemplate saveEmailTemplate(String docType, String subject, String body);

  Models.DocTemplate getDocTemplate(String docType);

  Models.DocTemplate saveDocTemplate(String docType, String html);

  default java.util.List<Models.Template> listTemplates() {
    return listTemplates(null);
  }

  default java.util.List<Models.Template> listTemplates(Models.TemplateKind kind) {
    throw new UnsupportedOperationException("listTemplates non disponible dans " + getLabel());
  }

  default Models.Template saveTemplate(Models.Template template) {
    throw new UnsupportedOperationException("saveTemplate non disponible dans " + getLabel());
  }

  default void deleteTemplate(String templateId) {
    throw new UnsupportedOperationException("deleteTemplate non disponible dans " + getLabel());
  }

  default void sendEmail(
      java.util.List<String> to,
      java.util.List<String> cc,
      java.util.List<String> bcc,
      String subject,
      String html) {
    throw new UnsupportedOperationException("sendEmail non disponible dans " + getLabel());
  }

  default void sendEmail(String to, String subject, String html) {
    if (to == null || to.isBlank()) {
      throw new IllegalArgumentException("Destinataire requis");
    }
    sendEmail(java.util.List.of(to), java.util.List.of(), java.util.List.of(), subject, html);
  }

  default void emailDocsBatch(
      java.util.List<String> ids, String to, String subject, String message) {
    emailDocsBatch(ids, to, subject, message, true);
  }

  void emailDocsBatch(
      java.util.List<String> ids, String to, String subject, String message, boolean attachPdf);

  default Models.Doc createQuoteFromIntervention(Models.Intervention intervention) {
    if (intervention == null) {
      throw new IllegalArgumentException("Intervention requise");
    }
    String clientId = intervention.clientId();
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException(
          "Intervention sans client — impossible de générer un devis");
    }
    String agencyId = intervention.agencyId();
    if (agencyId == null || agencyId.isBlank()) {
      agencyId = getCurrentAgencyId();
    }
    if (agencyId == null || agencyId.isBlank()) {
      throw new IllegalStateException(
          "Agence courante inconnue — impossible de générer un devis");
    }
    String title = intervention.title();
    if (title == null || title.isBlank()) {
      title = intervention.id() == null ? "Intervention" : "Intervention " + intervention.id();
    }
    Models.Doc doc = createDoc("QUOTE", agencyId, clientId, title);
    java.util.List<String> resourceIds = intervention.resourceIds();
    if (resourceIds == null || resourceIds.isEmpty()) {
      return doc;
    }
    java.util.Map<String, Models.Resource> resourcesById = new java.util.HashMap<>();
    for (Models.Resource resource : listResources()) {
      resourcesById.put(resource.id(), resource);
    }
    java.util.List<Models.DocLine> lines = new java.util.ArrayList<>();
    for (String resourceId : resourceIds) {
      Models.Resource resource = resourcesById.get(resourceId);
      String designation = resource != null ? resource.name() : "Ressource " + resourceId;
      double rate;
      try {
        rate = getResourceDailyRate(resourceId);
      } catch (UnsupportedOperationException ex) {
        rate = 0.0;
      }
      if (Double.isNaN(rate) || Double.isInfinite(rate)) {
        rate = 0.0;
      }
      lines.add(new Models.DocLine(designation, 1.0, rate, 0.0));
    }
    if (lines.isEmpty()) {
      return doc;
    }
    Models.Doc docWithLines =
        new Models.Doc(
            doc.id(),
            doc.type(),
            doc.status(),
            doc.reference(),
            doc.title(),
            doc.agencyId(),
            doc.clientId(),
            doc.date(),
            doc.totalHt(),
            doc.totalVat(),
            doc.totalTtc(),
            doc.delivered(),
            doc.paid(),
            lines);
    return updateDoc(docWithLines);
  }

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

  default java.util.List<String> getInterventionTags(String interventionId) {
    throw new UnsupportedOperationException("getInterventionTags non disponible dans " + getLabel());
  }

  default void setInterventionTags(String interventionId, java.util.List<String> tags) {
    throw new UnsupportedOperationException("setInterventionTags non disponible dans " + getLabel());
  }

  default java.util.List<String> suggestTags(int limit) {
    return java.util.List.of();
  }

  default java.util.List<String> suggestTags() {
    return suggestTags(20);
  }

  static String merge(String template, java.util.Map<String, String> context) {
    if (template == null || template.isBlank()) {
      return "";
    }
    if (context == null || context.isEmpty()) {
      return template;
    }
    String merged = template;
    for (java.util.Map.Entry<String, String> entry : context.entrySet()) {
      String value = entry.getValue() == null ? "" : entry.getValue();
      merged = merged.replace("{{" + entry.getKey() + "}}", value);
    }
    return merged;
  }

  static String normalizeTemplateKey(String templateKey) {
    return (templateKey == null || templateKey.isBlank()) ? "default" : templateKey;
  }
}
