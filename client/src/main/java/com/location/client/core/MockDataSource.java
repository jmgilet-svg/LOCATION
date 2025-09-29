package com.location.client.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MockDataSource implements DataSourceProvider {

  private final List<Models.Agency> agencies = new ArrayList<>();
  private final List<Models.Client> clients = new ArrayList<>();
  private final List<Models.Driver> drivers = new ArrayList<>();
  private String currentAgencyId;
  private final List<Models.Resource> resources = new ArrayList<>();
  private final List<Models.ResourceType> resourceTypes = new ArrayList<>();
  private final Map<String, String> resourceTypeAssignments = new HashMap<>();
  private final Map<String, Double> resourceDailyRates = new HashMap<>();
  private final List<Models.Intervention> interventions = new ArrayList<>();
  private final List<Models.Unavailability> unavailabilities = new ArrayList<>();
  private final List<Models.RecurringUnavailability> recurring = new ArrayList<>();
  private final Map<String, Map<String, Models.EmailTemplate>> agencyTemplates = new HashMap<>();
  private final Map<String, Map<String, Integer>> docSequences = new HashMap<>();
  private final Map<String, Map<String, Models.EmailTemplate>> docTemplates = new HashMap<>();
  private final Map<String, Map<String, Models.DocTemplate>> docHtmlTemplates = new HashMap<>();
  private static final Pattern DOC_REF_PATTERN = Pattern.compile("(DV|BC|BL|FA)-(\\d{4})-(\\d{4})");
  private final List<Models.Doc> docs = new ArrayList<>();

  public MockDataSource() {
    resetDemoData();
  }

  private boolean matchesCurrentAgency(String agencyId) {
    if (currentAgencyId == null || currentAgencyId.isBlank()) {
      return true;
    }
    return agencyId != null && agencyId.equals(currentAgencyId);
  }

  private List<Models.Resource> resourcesForCurrentAgency() {
    return resources.stream().filter(r -> matchesCurrentAgency(r.agencyId())).toList();
  }

  private Set<String> resourceIdsForCurrentAgency() {
    return resourcesForCurrentAgency().stream().map(Models.Resource::id).collect(Collectors.toSet());
  }

  @Override
  public String getLabel() {
    return "MOCK";
  }

  @Override
  public void resetDemoData() {
    agencies.clear();
    clients.clear();
    drivers.clear();
    resources.clear();
    resourceTypes.clear();
    resourceTypeAssignments.clear();
    resourceDailyRates.clear();
    interventions.clear();
    unavailabilities.clear();
    recurring.clear();
    agencyTemplates.clear();
    docSequences.clear();
    docTemplates.clear();
    docHtmlTemplates.clear();
    docs.clear();

    var a1 = new Models.Agency(UUID.randomUUID().toString(), "Agence Nord");
    var a2 = new Models.Agency(UUID.randomUUID().toString(), "Agence Sud");
    agencies.add(a1);
    agencies.add(a2);
    currentAgencyId = a1.id();
    String defaultKey = DataSourceProvider.normalizeTemplateKey(null);
    agencyTemplates
        .computeIfAbsent(a1.id(), k -> new HashMap<>())
        .put(
            defaultKey,
            new Models.EmailTemplate(
                defaultKey,
                "Intervention {{interventionTitle}}",
                "Bonjour {{clientName}},\nVeuillez trouver la fiche.\nAgence : {{agencyName}}\nDu {{start}} au {{end}}"));
    agencyTemplates
        .computeIfAbsent(a2.id(), k -> new HashMap<>())
        .put(
            defaultKey,
            new Models.EmailTemplate(
                defaultKey,
                "Intervention {{interventionTitle}}",
                "Bonjour {{clientName}},\nVeuillez trouver la fiche.\nAgence : {{agencyName}}\nDu {{start}} au {{end}}"));

    docTemplates
        .computeIfAbsent(a1.id(), k -> new HashMap<>())
        .put(
            "QUOTE",
            new Models.EmailTemplate(
                "QUOTE",
                "Devis {{docRef}}",
                "Bonjour {{clientName}},\nVeuillez trouver le devis {{docRef}} concernant {{docTitle}}."));
    docTemplates
        .computeIfAbsent(a1.id(), k -> new HashMap<>())
        .put(
            "INVOICE",
            new Models.EmailTemplate(
                "INVOICE",
                "Facture {{docRef}}",
                "Bonjour {{clientName}},\nVeuillez trouver votre facture {{docRef}} (montant TTC : {{totalTtc}} €)."));

    docHtmlTemplates
        .computeIfAbsent(a1.id(), k -> new HashMap<>())
        .put("QUOTE", new Models.DocTemplate("<h1>Devis {{docRef}}</h1>"));
    docHtmlTemplates
        .computeIfAbsent(a1.id(), k -> new HashMap<>())
        .put("INVOICE", new Models.DocTemplate("<h1>Facture {{docRef}}</h1>"));

    Models.ResourceType typeGrue =
        saveResourceType(new Models.ResourceType(null, "Grue", "crane.svg"));
    Models.ResourceType typeCamion =
        saveResourceType(new Models.ResourceType(null, "Camion", "truck.svg"));
    Models.ResourceType typeRemorque =
        saveResourceType(new Models.ResourceType(null, "Remorque", "trailer.svg"));

    clients.add(new Models.Client(UUID.randomUUID().toString(), "Client Alpha", "alpha@acme.tld"));
    clients.add(new Models.Client(UUID.randomUUID().toString(), "Client Beta", "beta@acme.tld"));

    drivers.add(new Models.Driver(UUID.randomUUID().toString(), "Jean Dupont", "jean@loc.tld"));
    drivers.add(new Models.Driver(UUID.randomUUID().toString(), "Sophie Martin", "sophie@loc.tld"));

    Models.Resource resGrue =
        new Models.Resource(
            UUID.randomUUID().toString(),
            "Grue X",
            "AB-123-CD",
            0xFF4444,
            a1.id(),
            "grue,90t",
            90);
    Models.Resource resCamion =
        new Models.Resource(
            UUID.randomUUID().toString(),
            "Camion Y",
            "EF-456-GH",
            0x44AA44,
            a1.id(),
            "camion,benne",
            18);
    Models.Resource resRemorque =
        new Models.Resource(
            UUID.randomUUID().toString(),
            "Remorque Z",
            "IJ-789-KL",
            0x4444FF,
            a2.id(),
            "remorque,plateau",
            10);
    resources.add(resGrue);
    resources.add(resCamion);
    resources.add(resRemorque);

    resourceTypeAssignments.put(resGrue.id(), typeGrue.id());
    resourceTypeAssignments.put(resCamion.id(), typeCamion.id());
    resourceTypeAssignments.put(resRemorque.id(), typeRemorque.id());
    resourceDailyRates.put(resGrue.id(), 950.0);
    resourceDailyRates.put(resCamion.id(), 520.0);
    resourceDailyRates.put(resRemorque.id(), 310.0);

    ZonedDateTime base =
        ZonedDateTime.now(ZoneId.systemDefault()).withHour(9).withMinute(0).withSecond(0).withNano(0);

    addIntervention(
        a1.id(),
        resources.get(0).id(),
        clients.get(0).id(),
        drivers.get(0).id(),
        "Livraison chantier",
        base.plusDays(1).toInstant(),
        base.plusDays(1).plusHours(2).toInstant(),
        "Site A – prévoir EPI");
    addIntervention(
        a1.id(),
        resources.get(1).id(),
        clients.get(1).id(),
        drivers.get(1).id(),
        "Levage poutres",
        base.plusDays(1).plusHours(3).toInstant(),
        base.plusDays(1).plusHours(5).toInstant(),
        null);
    addIntervention(
        a2.id(),
        resources.get(2).id(),
        clients.get(1).id(),
        null,
        "Transport matériel",
        base.plusDays(2).toInstant(),
        base.plusDays(2).plusHours(1).toInstant(),
        "RDV à 7h30");

    addUnavailability(
        resources.get(0).id(),
        "Maintenance",
        base.plusDays(1).plusHours(6).toInstant(),
        base.plusDays(1).plusHours(8).toInstant());
    addUnavailability(
        resources.get(1).id(),
        "Panne hydraulique",
        base.plusDays(1).minusHours(2).toInstant(),
        base.plusDays(1).minusHours(1).toInstant());

    recurring.add(
        new Models.RecurringUnavailability(
            UUID.randomUUID().toString(),
            resources.get(0).id(),
            DayOfWeek.MONDAY,
            LocalTime.of(8, 0),
            LocalTime.of(10, 0),
            "Routine hebdo"));

    List<Models.DocLine> quoteLines = List.of(new Models.DocLine("Heures grue", 2.0, 120.0, 20.0));
    java.time.OffsetDateTime quoteDate = java.time.OffsetDateTime.now().minusDays(5);
    docs.add(
        buildDoc(
            UUID.randomUUID().toString(),
            "QUOTE",
            "DRAFT",
            nextReference(a1.id(), "QUOTE", quoteDate),
            "Levage chantier",
            a1.id(),
            clients.get(0).id(),
            quoteDate,
            quoteLines));
    List<Models.DocLine> invoiceLines = List.of(new Models.DocLine("Transport", 1.0, 80.0, 20.0));
    java.time.OffsetDateTime invoiceDate = java.time.OffsetDateTime.now().minusDays(2);
    docs.add(
        buildDoc(
            UUID.randomUUID().toString(),
            "INVOICE",
            "ISSUED",
            nextReference(a1.id(), "INVOICE", invoiceDate),
            "Transport X",
            a1.id(),
            clients.get(1).id(),
            invoiceDate,
            invoiceLines));
  }

  @Override
  public void resetDemo() {
    resetDemoData();
  }

  @Override
  public List<Models.Agency> listAgencies() {
    return List.copyOf(agencies);
  }

  @Override
  public List<Models.Client> listClients() {
    return List.copyOf(clients);
  }

  @Override
  public String getCurrentAgencyId() {
    return currentAgencyId;
  }

  @Override
  public void setCurrentAgencyId(String agencyId) {
    this.currentAgencyId = agencyId;
  }

  @Override
  public List<Models.Resource> listResources() {
    return resourcesForCurrentAgency();
  }

  @Override
  public List<Models.ResourceType> listResourceTypes() {
    return resourceTypes.stream()
        .sorted(Comparator.comparing(Models.ResourceType::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @Override
  public Models.ResourceType saveResourceType(Models.ResourceType resourceType) {
    if (resourceType == null || resourceType.name() == null || resourceType.name().isBlank()) {
      throw new IllegalArgumentException("Nom de type requis");
    }
    String id =
        resourceType.id() == null || resourceType.id().isBlank()
            ? UUID.randomUUID().toString()
            : resourceType.id();
    String icon = resourceType.iconName() == null ? "hook.svg" : resourceType.iconName();
    Models.ResourceType updated = new Models.ResourceType(id, resourceType.name().trim(), icon);
    resourceTypes.removeIf(t -> t.id().equals(id));
    resourceTypes.add(updated);
    resourceTypes.sort(Comparator.comparing(Models.ResourceType::name, String.CASE_INSENSITIVE_ORDER));
    return updated;
  }

  @Override
  public void deleteResourceType(String id) {
    if (id == null) {
      return;
    }
    resourceTypes.removeIf(t -> t.id().equals(id));
    resourceTypeAssignments.entrySet().removeIf(e -> id.equals(e.getValue()));
  }

  @Override
  public String getResourceTypeForResource(String resourceId) {
    return resourceTypeAssignments.get(resourceId);
  }

  @Override
  public void setResourceTypeForResource(String resourceId, String resourceTypeId) {
    if (resourceTypeId == null || resourceTypeId.isBlank()) {
      resourceTypeAssignments.remove(resourceId);
      return;
    }
    boolean exists = resourceTypes.stream().anyMatch(t -> t.id().equals(resourceTypeId));
    if (!exists) {
      throw new IllegalArgumentException("Type de ressource inconnu");
    }
    resourceTypeAssignments.put(resourceId, resourceTypeId);
  }

  @Override
  public double getResourceDailyRate(String resourceId) {
    return resourceDailyRates.getOrDefault(resourceId, 0.0);
  }

  @Override
  public void setResourceDailyRate(String resourceId, double rate) {
    double sanitized = Double.isNaN(rate) || rate < 0 ? 0.0 : rate;
    resourceDailyRates.put(resourceId, sanitized);
  }

  @Override
  public Models.Resource saveResource(Models.Resource resource) {
    String id = "";
    Models.Resource existing =
        id == null
            ? null
            : resources.stream().filter(r -> r.id().equals(resource.id())).findFirst().orElse(null);
    if (existing == null && resource.id() == null) {
      id = UUID.randomUUID().toString();
    } else {
    	id =resource.id();
    }
    if (existing != null) {
      resources.remove(existing);
    }
    String agencyId =
        resource.agencyId() != null
            ? resource.agencyId()
            : existing != null ? existing.agencyId() : getCurrentAgencyId();
    if ((agencyId == null || agencyId.isBlank()) && !agencies.isEmpty()) {
      agencyId = agencies.get(0).id();
    }
    Integer color = resource.colorRgb() != null ? resource.colorRgb() : existing != null ? existing.colorRgb() : null;
    Models.Resource saved =
        new Models.Resource(
            id,
            resource.name(),
            resource.licensePlate(),
            color,
            agencyId,
            resource.tags(),
            resource.capacityTons());
    resources.add(saved);
    return saved;
  }

  @Override
  public List<Models.Intervention> listInterventions(
      java.time.OffsetDateTime from, java.time.OffsetDateTime to, String resourceId) {
    Instant fromInstant = from != null ? from.toInstant() : null;
    Instant toInstant = to != null ? to.toInstant() : null;
    return interventions.stream()
        .filter(i -> matchesCurrentAgency(i.agencyId()))
        .filter(i -> resourceId == null || resourceId.equals(i.resourceId()))
        .filter(
            i ->
                (fromInstant == null || i.end().isAfter(fromInstant))
                    && (toInstant == null || i.start().isBefore(toInstant)))
        .toList();
  }

  @Override
  public Models.Intervention createIntervention(Models.Intervention intervention) {
    boolean overlap =
        interventions.stream()
            .anyMatch(
                i ->
                    conflicts(
                        i,
                        intervention.resourceId(),
                        intervention.driverId(),
                        intervention.start(),
                        intervention.end()));
    if (overlap) {
      throw new IllegalStateException("Conflit d'affectation (MOCK)");
    }
    boolean unavailable =
        listUnavailabilities(
                intervention.start().atOffset(ZoneOffset.UTC),
                intervention.end().atOffset(ZoneOffset.UTC),
                intervention.resourceId())
            .stream()
            .anyMatch(
                u -> u.end().isAfter(intervention.start()) && u.start().isBefore(intervention.end()));
    if (unavailable) {
      throw new IllegalStateException("Ressource indisponible (MOCK)");
    }
    Models.Intervention created =
        new Models.Intervention(
            UUID.randomUUID().toString(),
            intervention.agencyId(),
            intervention.resourceId(),
            intervention.clientId(),
            intervention.driverId(),
            intervention.title(),
            intervention.start(),
            intervention.end(),
            intervention.notes());
    interventions.add(created);
    return created;
  }

  @Override
  public Models.Intervention updateIntervention(Models.Intervention intervention) {
    boolean overlap =
        interventions.stream()
            .filter(i -> !i.id().equals(intervention.id()))
            .anyMatch(
                i ->
                    conflicts(
                        i,
                        intervention.resourceId(),
                        intervention.driverId(),
                        intervention.start(),
                        intervention.end()));
    if (overlap) {
      throw new IllegalStateException("Conflit (MOCK) avec une autre intervention");
    }
    boolean unavailable =
        listUnavailabilities(
                intervention.start().atOffset(ZoneOffset.UTC),
                intervention.end().atOffset(ZoneOffset.UTC),
                intervention.resourceId())
            .stream()
            .anyMatch(
                u -> u.end().isAfter(intervention.start()) && u.start().isBefore(intervention.end()));
    if (unavailable) {
      throw new IllegalStateException("Conflit (MOCK) indisponibilité");
    }
    for (int i = 0; i < interventions.size(); i++) {
      if (interventions.get(i).id().equals(intervention.id())) {
        interventions.set(i, intervention);
        return intervention;
      }
    }
    throw new IllegalArgumentException("Intervention inconnue");
  }

  @Override
  public void deleteIntervention(String id) {
    interventions.removeIf(i -> i.id().equals(id));
  }

  @Override
  public List<Models.Unavailability> listUnavailabilities(
      java.time.OffsetDateTime from, java.time.OffsetDateTime to, String resourceId) {
    Instant fromInstant = from != null ? from.toInstant() : null;
    Instant toInstant = to != null ? to.toInstant() : null;
    List<Models.Unavailability> result = new ArrayList<>();
    Set<String> allowedResources = resourceIdsForCurrentAgency();
    boolean enforceAgency = currentAgencyId != null && !currentAgencyId.isBlank();
    for (Models.Unavailability u : unavailabilities) {
      if (enforceAgency && !allowedResources.contains(u.resourceId())) {
        continue;
      }
      if ((resourceId == null || resourceId.equals(u.resourceId()))
          && overlaps(u.start(), u.end(), fromInstant, toInstant)) {
        result.add(u);
      }
    }
    if (fromInstant != null && toInstant != null) {
      LocalDate day = fromInstant.atZone(ZoneOffset.UTC).toLocalDate();
      LocalDate end = toInstant.atZone(ZoneOffset.UTC).toLocalDate();
      while (!day.isAfter(end)) {
        for (Models.RecurringUnavailability ru : recurring) {
          if (enforceAgency && !allowedResources.contains(ru.resourceId())) {
            continue;
          }
          if (resourceId != null && !resourceId.equals(ru.resourceId())) {
            continue;
          }
          if (ru.dayOfWeek() == day.getDayOfWeek()) {
            Instant start =
                day.atTime(ru.start()).atOffset(ZoneOffset.UTC).toInstant();
            Instant endInstant =
                day.atTime(ru.end()).atOffset(ZoneOffset.UTC).toInstant();
            if (overlaps(start, endInstant, fromInstant, toInstant)) {
              result.add(
                  new Models.Unavailability(
                      "ru:" + ru.id() + ":" + day,
                      ru.resourceId(),
                      ru.reason(),
                      start,
                      endInstant,
                      true));
            }
          }
        }
        day = day.plusDays(1);
      }
    }
    return result;
  }

  private void validateUnavailabilityWindow(
      String resourceId, Instant start, Instant end, String ignoreId) {
    if (start == null || end == null || !end.isAfter(start)) {
      throw new IllegalArgumentException("Plage horaire invalide (MOCK)");
    }
    List<Models.Unavailability> existing =
        listUnavailabilities(start.atOffset(ZoneOffset.UTC), end.atOffset(ZoneOffset.UTC), resourceId);
    boolean overlapUnavailability =
        existing.stream()
            .filter(u -> ignoreId == null || !ignoreId.equals(u.id()))
            .anyMatch(u -> u.end().isAfter(start) && u.start().isBefore(end));
    if (overlapUnavailability) {
      throw new IllegalStateException("Chevauche une indisponibilité existante (MOCK)");
    }
    boolean overlapIntervention =
        interventions.stream()
            .filter(i -> ignoreId == null || !ignoreId.equals(i.id()))
            .anyMatch(
                i ->
                    i.resourceId().equals(resourceId)
                        && i.end().isAfter(start)
                        && i.start().isBefore(end));
    if (overlapIntervention) {
      throw new IllegalStateException("Chevauche une intervention existante (MOCK)");
    }
  }

  @Override
  public Models.Unavailability createUnavailability(Models.Unavailability unavailability) {
    validateUnavailabilityWindow(
        unavailability.resourceId(), unavailability.start(), unavailability.end(), null);
    Models.Unavailability created =
        new Models.Unavailability(
            UUID.randomUUID().toString(),
            unavailability.resourceId(),
            unavailability.reason(),
            unavailability.start(),
            unavailability.end(),
            false);
    unavailabilities.add(created);
    return created;
  }

  @Override
  public java.util.List<Models.Unavailability> listUnavailability(String resourceId) {
    return unavailabilities.stream()
        .filter(u -> resourceId == null || resourceId.equals(u.resourceId()))
        .sorted(java.util.Comparator.comparing(Models.Unavailability::start))
        .toList();
  }

  @Override
  public Models.Unavailability saveUnavailability(Models.Unavailability unavailability) {
    String id = unavailability.id();
    if (id == null) {
      return createUnavailability(unavailability);
    }
    validateUnavailabilityWindow(
        unavailability.resourceId(), unavailability.start(), unavailability.end(), id);
    Models.Unavailability existing =
        unavailabilities.stream().filter(u -> u.id().equals(id)).findFirst().orElse(null);
    if (existing != null) {
      unavailabilities.remove(existing);
    }
    Models.Unavailability updated =
        new Models.Unavailability(
            id,
            unavailability.resourceId(),
            unavailability.reason(),
            unavailability.start(),
            unavailability.end(),
            false);
    unavailabilities.add(updated);
    return updated;
  }

  @Override
  public void deleteUnavailability(String id) {
    if (id == null) {
      return;
    }
    unavailabilities.removeIf(u -> u.id().equals(id));
  }

  @Override
  public List<Models.RecurringUnavailability> listRecurringUnavailabilities(String resourceId) {
    Set<String> allowedResources = resourceIdsForCurrentAgency();
    boolean enforceAgency = currentAgencyId != null && !currentAgencyId.isBlank();
    return recurring.stream()
        .filter(r -> !enforceAgency || allowedResources.contains(r.resourceId()))
        .filter(r -> resourceId == null || resourceId.equals(r.resourceId()))
        .toList();
  }

  @Override
  public Models.RecurringUnavailability createRecurringUnavailability(
      Models.RecurringUnavailability recurringUnavailability) {
    if (!recurringUnavailability.start().isBefore(recurringUnavailability.end())) {
      throw new IllegalArgumentException("Heure début < heure fin (MOCK)");
    }
    Models.RecurringUnavailability created =
        new Models.RecurringUnavailability(
            UUID.randomUUID().toString(),
            recurringUnavailability.resourceId(),
            recurringUnavailability.dayOfWeek(),
            recurringUnavailability.start(),
            recurringUnavailability.end(),
            recurringUnavailability.reason());
    recurring.add(created);
    return created;
  }

  @Override
  public Models.RecurringUnavailability saveRecurringUnavailability(
      Models.RecurringUnavailability recurringUnavailability) {
    if (recurringUnavailability.id() == null) {
      return createRecurringUnavailability(recurringUnavailability);
    }
    if (!recurringUnavailability.start().isBefore(recurringUnavailability.end())) {
      throw new IllegalArgumentException("Heure début < heure fin (MOCK)");
    }
    recurring.removeIf(r -> r.id().equals(recurringUnavailability.id()));
    Models.RecurringUnavailability updated =
        new Models.RecurringUnavailability(
            recurringUnavailability.id(),
            recurringUnavailability.resourceId(),
            recurringUnavailability.dayOfWeek(),
            recurringUnavailability.start(),
            recurringUnavailability.end(),
            recurringUnavailability.reason());
    recurring.add(updated);
    return updated;
  }

  @Override
  public void deleteRecurringUnavailability(String id) {
    if (id == null) {
      return;
    }
    recurring.removeIf(r -> r.id().equals(id));
  }

  @Override
  public Path downloadResourcesCsv(String tags, Path target) {
    try {
      List<Models.Resource> data = new ArrayList<>(resourcesForCurrentAgency());
      if (tags != null && !tags.isBlank()) {
        Set<String> requested =
            java.util.Arrays.stream(tags.toLowerCase().split("\\s*,\\s*"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        if (!requested.isEmpty()) {
          data =
              data.stream()
                  .filter(
                      r ->
                          r.tags() != null
                              && requested.stream()
                                  .allMatch(t -> r.tags().toLowerCase().contains(t)))
                  .toList();
        }
      }
      StringBuilder sb =
          new StringBuilder("id;name;licensePlate;capacityTons;tags;agencyId\n");
      for (Models.Resource r : data) {
        sb.append(r.id())
            .append(';')
            .append(r.name())
            .append(';')
            .append(r.licensePlate() == null ? "" : r.licensePlate())
            .append(';')
            .append(r.capacityTons() == null ? "" : r.capacityTons())
            .append(';')
            .append(r.tags() == null ? "" : r.tags())
            .append(';')
            .append(r.agencyId())
            .append('\n');
      }
      Files.writeString(target, sb.toString());
      return target;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public java.nio.file.Path downloadInterventionPdf(String interventionId, java.nio.file.Path target) {
    throw new UnsupportedOperationException(
        "Export PDF non disponible en mode Mock (aucune écriture disque).");
  }

  @Override
  public void emailInterventionPdf(String interventionId, String to, String subject, String message) {
    // Simulation instantanée : pas d'envoi réel en mode Mock.
  }

  @Override
  public java.nio.file.Path downloadInterventionsCsv(
      java.time.OffsetDateTime from, java.time.OffsetDateTime to, java.nio.file.Path target) {
    throw new UnsupportedOperationException(
        "Export Interventions CSV indisponible en mode Mock (pas d'écriture fichier).");
  }

  @Override
  public java.nio.file.Path downloadClientsCsv(java.nio.file.Path target) {
    throw new UnsupportedOperationException(
        "Export Clients CSV indisponible en mode Mock (pas d'écriture fichier).");
  }

  @Override
  public java.nio.file.Path downloadUnavailabilitiesCsv(
      java.time.OffsetDateTime from,
      java.time.OffsetDateTime to,
      String resourceId,
      java.nio.file.Path target) {
    throw new UnsupportedOperationException(
        "Export Indisponibilités CSV indisponible en mode Mock (pas d'écriture fichier).");
  }

  @Override
  public java.util.Map<String, Boolean> getServerFeatures() {
    java.util.HashMap<String, Boolean> features = new java.util.HashMap<>();
    features.put("FEATURE_EMAIL_BULK", true);
    features.put("FEATURE_RESOURCES_CSV", true);
    features.put("FEATURE_INTERVENTION_PDF", true);
    features.put("FEATURE_CLIENTS_CSV", true);
    features.put("FEATURE_UNAVAILABILITIES_CSV", true);
    return features;

  }

  @Override
  public java.util.List<Models.Doc> listDocs(String type, String clientId) {
    return docs.stream()
        .filter(d -> matchesCurrentAgency(d.agencyId()))
        .filter(d -> type == null || type.isBlank() || d.type().equals(type))
        .filter(d -> clientId == null || clientId.isBlank() || d.clientId().equals(clientId))
        .toList();
  }

  @Override
  public Models.Doc createDoc(String type, String agencyId, String clientId, String title) {
    java.time.OffsetDateTime date = java.time.OffsetDateTime.now();
    String reference = "QUOTE".equalsIgnoreCase(type) ? nextReference(agencyId, type, date) : null;
    Models.Doc created =
        new Models.Doc(
            java.util.UUID.randomUUID().toString(),
            type,
            "DRAFT",
            reference,
            title,
            agencyId,
            clientId,
            date,
            0,
            0,
            0,
            java.util.List.of());
    docs.add(created);
    return created;
  }

  @Override
  public Models.Doc updateDoc(Models.Doc document) {
    Models.Doc updated =
        buildDoc(
            document.id(),
            document.type(),
            document.status(),
            document.reference(),
            document.title(),
            document.agencyId(),
            document.clientId(),
            document.date(),
            document.lines());
    replaceDoc(updated);
    return updated;
  }

  @Override
  public void deleteDoc(String id) {
    docs.removeIf(d -> d.id().equals(id));
  }

  @Override
  public Models.Doc transitionDoc(String id, String toType) {
    Models.Doc source =
        docs.stream()
            .filter(d -> d.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Document introuvable"));
    String status = "INVOICE".equalsIgnoreCase(toType) ? "ISSUED" : "DRAFT";
    java.time.OffsetDateTime date = java.time.OffsetDateTime.now();
    Models.Doc copy =
        buildDoc(
            java.util.UUID.randomUUID().toString(),
            toType,
            status,
            nextReference(source.agencyId(), toType, date),
            source.title(),
            source.agencyId(),
            source.clientId(),
            date,
            source.lines());
    docs.add(copy);
    return copy;
  }

  @Override
  public java.nio.file.Path downloadDocsCsv(String type, String clientId, java.nio.file.Path target) {
    throw new UnsupportedOperationException(
        "Export CSV disponible uniquement en mode REST (Mock n'écrit pas de fichier).");
  }

  @Override
  public Models.EmailTemplate getEmailTemplate(String docType) {
    var templates = docTemplates.getOrDefault(currentAgencyId, java.util.Map.of());
    String key = DataSourceProvider.normalizeTemplateKey(docType);
    return templates.getOrDefault(docType, new Models.EmailTemplate(key, "", ""));
  }

  @Override
  public Models.EmailTemplate saveEmailTemplate(String docType, String subject, String body) {
    if (currentAgencyId == null || currentAgencyId.isBlank()) {
      throw new IllegalStateException("Agence courante non définie (Mock)");
    }
    String key = DataSourceProvider.normalizeTemplateKey(docType);
    Models.EmailTemplate template = new Models.EmailTemplate(key, subject, body);
    docTemplates.computeIfAbsent(currentAgencyId, k -> new HashMap<>()).put(docType, template);
    return template;
  }

  @Override
  public Models.DocTemplate getDocTemplate(String docType) {
    var templates = docHtmlTemplates.getOrDefault(currentAgencyId, java.util.Map.of());
    return templates.getOrDefault(docType, new Models.DocTemplate(""));
  }

  @Override
  public Models.DocTemplate saveDocTemplate(String docType, String html) {
    if (currentAgencyId == null || currentAgencyId.isBlank()) {
      throw new IllegalStateException("Agence courante non définie (Mock)");
    }
    Models.DocTemplate template = new Models.DocTemplate(html);
    docHtmlTemplates.computeIfAbsent(currentAgencyId, k -> new HashMap<>()).put(docType, template);
    return template;
  }

  @Override
  public java.nio.file.Path downloadDocPdf(String id, java.nio.file.Path target) {
    throw new UnsupportedOperationException(
        "Export PDF docs indisponible en mode Mock (pas d'écriture disque).");
  }

  @Override
  public void emailDoc(String id, String to, String subject, String message) {
    // Simulation : aucun envoi réel en mode Mock.
  }

  @Override
  public void emailDocsBatch(java.util.List<String> ids, String to, String subject, String message) {
    // Simulation : aucun envoi réel en mode Mock.
  }

  @Override
  public Models.EmailTemplate getAgencyEmailTemplate(String agencyId, String templateKey) {
    String key = DataSourceProvider.normalizeTemplateKey(templateKey);
    var templates = agencyTemplates.getOrDefault(agencyId, java.util.Map.of());
    Models.EmailTemplate template = templates.get(key);
    if (template != null) {
      return template;
    }
    return new Models.EmailTemplate(
        key, "[LOCATION] " + key, "<p>Bonjour,</p><p>Veuillez trouver le document en PJ.</p>");
  }

  @Override
  public Models.EmailTemplate updateAgencyEmailTemplate(
      String agencyId, String templateKey, String subject, String html) {
    String key = DataSourceProvider.normalizeTemplateKey(templateKey);
    Models.EmailTemplate template = new Models.EmailTemplate(key, subject, html);
    agencyTemplates.computeIfAbsent(agencyId, k -> new HashMap<>()).put(key, template);
    return template;
  }

  @Override
  public void emailBulk(java.util.List<String> recipients, String subject, String html) {
    // Simulation : aucun envoi réel en mode Mock.
  }

  @Override
  public void emailBulk(java.util.List<String> ids, String toOverride) {
    // Simulation : aucun envoi réel en mode Mock.
  }

  private Models.Doc buildDoc(
      String id,
      String type,
      String status,
      String reference,
      String title,
      String agencyId,
      String clientId,
      java.time.OffsetDateTime date,
      java.util.List<Models.DocLine> lines) {
      double totalHt = 0;
      double totalVat = 0;
      for (Models.DocLine line : lines) {
        double lineHt = line.quantity() * line.unitPrice();
        totalHt += lineHt;
        totalVat += lineHt * (line.vatRate() / 100.0);
      }
    Models.Doc doc =
        new Models.Doc(
            id,
            type,
            status,
            reference,
            title,
            agencyId,
            clientId,
            date != null ? date : java.time.OffsetDateTime.now(),
            round2(totalHt),
            round2(totalVat),
            round2(totalHt + totalVat),
            java.util.List.copyOf(lines));
    recordReference(agencyId, type, reference, doc.date());
    return doc;
  }

  private String nextReference(String agencyId, String type, java.time.OffsetDateTime date) {
    String resolvedAgency = agencyId != null ? agencyId : currentAgencyId;
    if (resolvedAgency == null || resolvedAgency.isBlank()) {
      throw new IllegalStateException("Agence non définie pour la numérotation (Mock)");
    }
    int year = (date != null ? date.getYear() : java.time.OffsetDateTime.now().getYear());
    Map<String, Integer> perType = docSequences.computeIfAbsent(resolvedAgency, k -> new HashMap<>());
    String key = type.toUpperCase() + "-" + year;
    int next = perType.getOrDefault(key, 0) + 1;
    perType.put(key, next);
    String prefix = switch (type.toUpperCase()) {
      case "QUOTE" -> "DV";
      case "ORDER" -> "BC";
      case "DELIVERY" -> "BL";
      default -> "FA";
    };
    return String.format("%s-%d-%04d", prefix, year, next);
  }

  private void recordReference(
      String agencyId, String type, String reference, java.time.OffsetDateTime date) {
    if (agencyId == null || reference == null) {
      return;
    }
    var matcher = DOC_REF_PATTERN.matcher(reference);
    if (!matcher.matches()) {
      return;
    }
    int year = Integer.parseInt(matcher.group(2));
    int number = Integer.parseInt(matcher.group(3));
    Map<String, Integer> perType = docSequences.computeIfAbsent(agencyId, k -> new HashMap<>());
    String key = type.toUpperCase() + "-" + year;
    perType.merge(key, number, Math::max);
  }

  private void replaceDoc(Models.Doc updated) {
    for (int i = 0; i < docs.size(); i++) {
      if (docs.get(i).id().equals(updated.id())) {
        docs.set(i, updated);
        return;
      }
    }
    docs.add(updated);
  }

  private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private static boolean overlaps(Instant start, Instant end, Instant from, Instant to) {
    if (from == null || to == null) {
      return true;
    }
    return end.isAfter(from) && start.isBefore(to);
  }

  private boolean conflicts(
      Models.Intervention existing,
      String resourceId,
      String driverId,
      Instant start,
      Instant end) {
    if (!existing.end().isAfter(start) || !existing.start().isBefore(end)) {
      return false;
    }
    boolean sameResource = existing.resourceId().equals(resourceId);
    boolean sameDriver =
        existing.driverId() != null
            && driverId != null
            && existing.driverId().equals(driverId);
    return sameResource || sameDriver;
  }

  private void addIntervention(
      String agencyId,
      String resourceId,
      String clientId,
      String driverId,
      String title,
      Instant start,
      Instant end,
      String notes) {
    interventions.add(
        new Models.Intervention(
            UUID.randomUUID().toString(), agencyId, resourceId, clientId, driverId, title, start, end, notes));
  }

  private void addUnavailability(String resourceId, String reason, Instant start, Instant end) {
    unavailabilities.add(
        new Models.Unavailability(
            UUID.randomUUID().toString(), resourceId, reason, start, end, false));
  }

  @Override
  public void close() {}
}

