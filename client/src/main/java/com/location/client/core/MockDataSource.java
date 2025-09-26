package com.location.client.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MockDataSource implements DataSourceProvider {

  private final List<Models.Agency> agencies = new ArrayList<>();
  private final List<Models.Client> clients = new ArrayList<>();
  private final List<Models.Resource> resources = new ArrayList<>();
  private final List<Models.Intervention> interventions = new ArrayList<>();
  private final List<Models.Unavailability> unavailabilities = new ArrayList<>();

  public MockDataSource() {
    resetDemoData();
  }

  @Override
  public String getLabel() {
    return "MOCK";
  }

  @Override
  public void resetDemoData() {
    agencies.clear();
    clients.clear();
    resources.clear();
    interventions.clear();
    unavailabilities.clear();
    var a1 = new Models.Agency(UUID.randomUUID().toString(), "Agence 1");
    var a2 = new Models.Agency(UUID.randomUUID().toString(), "Agence 2");
    agencies.add(a1);
    agencies.add(a2);
    clients.add(new Models.Client(UUID.randomUUID().toString(), "Client Alpha", "facture@alpha.tld"));
    clients.add(new Models.Client(UUID.randomUUID().toString(), "Client Beta", "billing@beta.tld"));
    clients.add(new Models.Client(UUID.randomUUID().toString(), "Client Gamma", "compta@gamma.tld"));
    resources.add(
        new Models.Resource(UUID.randomUUID().toString(), "Camion X", "AB-123-CD", 0xFF4444, a1.id()));
    resources.add(
        new Models.Resource(UUID.randomUUID().toString(), "Grue Y", "EF-456-GH", 0x44FF44, a1.id()));
    resources.add(
        new Models.Resource(UUID.randomUUID().toString(), "Remorque Z", "IJ-789-KL", 0x4444FF, a2.id()));

    ZonedDateTime base =
        ZonedDateTime.now(ZoneId.systemDefault()).withHour(9).withMinute(0).withSecond(0).withNano(0);
    addIntervention(
        a1.id(),
        resources.get(0).id(),
        clients.get(0).id(),
        "Livraison chantier",
        base.plusDays(1).toInstant(),
        base.plusDays(1).plusHours(2).toInstant());
    addIntervention(
        a1.id(),
        resources.get(1).id(),
        clients.get(1).id(),
        "Levage poutres",
        base.plusDays(1).plusHours(3).toInstant(),
        base.plusDays(1).plusHours(5).toInstant());
    addIntervention(
        a2.id(),
        resources.get(2).id(),
        clients.get(2).id(),
        "Transport matériel",
        base.plusDays(2).toInstant(),
        base.plusDays(2).plusHours(1).toInstant());
    addUnavailability(
        resources.get(0).id(), "Maintenance", base.plusDays(1).plusHours(6).toInstant(), base.plusDays(1).plusHours(8).toInstant());
    addUnavailability(
        resources.get(1).id(), "Panne hydraulique", base.plusDays(1).minusHours(2).toInstant(), base.plusDays(1).minusHours(1).toInstant());
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
  public List<Models.Resource> listResources() {
    return List.copyOf(resources);
  }

  @Override
  public List<Models.Intervention> listInterventions(
      java.time.OffsetDateTime from, java.time.OffsetDateTime to, String resourceId) {
    Instant fromInstant = from != null ? from.toInstant() : null;
    Instant toInstant = to != null ? to.toInstant() : null;
    return interventions.stream()
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
                    i.resourceId().equals(intervention.resourceId())
                        && i.end().isAfter(intervention.start())
                        && i.start().isBefore(intervention.end()));
    if (overlap) {
      throw new IllegalStateException("Conflit d'affectation (MOCK)");
    }
    boolean unavailable =
        unavailabilities.stream()
            .anyMatch(
                u ->
                    u.resourceId().equals(intervention.resourceId())
                        && u.end().isAfter(intervention.start())
                        && u.start().isBefore(intervention.end()));
    if (unavailable) {
      throw new IllegalStateException("Ressource indisponible (MOCK)");
    }
    Models.Intervention created =
        new Models.Intervention(
            UUID.randomUUID().toString(),
            intervention.agencyId(),
            intervention.resourceId(),
            intervention.clientId(),
            intervention.title(),
            intervention.start(),
            intervention.end());
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
                    i.resourceId().equals(intervention.resourceId())
                        && i.end().isAfter(intervention.start())
                        && i.start().isBefore(intervention.end()));
    if (overlap) {
      throw new IllegalStateException("Conflit (MOCK) avec une autre intervention");
    }
    boolean unavailable =
        unavailabilities.stream()
            .anyMatch(
                u ->
                    u.resourceId().equals(intervention.resourceId())
                        && u.end().isAfter(intervention.start())
                        && u.start().isBefore(intervention.end()));
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
    return unavailabilities.stream()
        .filter(u -> resourceId == null || resourceId.equals(u.resourceId()))
        .filter(
            u ->
                (fromInstant == null || u.end().isAfter(fromInstant))
                    && (toInstant == null || u.start().isBefore(toInstant)))
        .toList();
  }

  @Override
  public Models.Unavailability createUnavailability(Models.Unavailability unavailability) {
    boolean overlapUnavailability =
        unavailabilities.stream()
            .anyMatch(
                u ->
                    u.resourceId().equals(unavailability.resourceId())
                        && u.end().isAfter(unavailability.start())
                        && u.start().isBefore(unavailability.end()));
    if (overlapUnavailability) {
      throw new IllegalStateException("Chevauche une indisponibilité existante (MOCK)");
    }
    boolean overlapIntervention =
        interventions.stream()
            .anyMatch(
                i ->
                    i.resourceId().equals(unavailability.resourceId())
                        && i.end().isAfter(unavailability.start())
                        && i.start().isBefore(unavailability.end()));
    if (overlapIntervention) {
      throw new IllegalStateException("Chevauche une intervention existante (MOCK)");
    }
    Models.Unavailability created =
        new Models.Unavailability(
            UUID.randomUUID().toString(),
            unavailability.resourceId(),
            unavailability.reason(),
            unavailability.start(),
            unavailability.end());
    unavailabilities.add(created);
    return created;
  }

  private void addIntervention(
      String agencyId, String resourceId, String clientId, String title, Instant start, Instant end) {
    interventions.add(
        new Models.Intervention(
            UUID.randomUUID().toString(), agencyId, resourceId, clientId, title, start, end));
  }

  private void addUnavailability(String resourceId, String reason, Instant start, Instant end) {
    unavailabilities.add(new Models.Unavailability(UUID.randomUUID().toString(), resourceId, reason, start, end));
  }

  @Override
  public void close() {}
}
