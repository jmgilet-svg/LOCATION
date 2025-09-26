package com.location.client.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

public class MockDataSource implements DataSourceProvider {

  private final List<Models.Agency> agencies = new ArrayList<>();
  private final List<Models.Client> clients = new ArrayList<>();
  private final List<Models.Resource> resources = new ArrayList<>();
  private final List<Models.Intervention> interventions = new ArrayList<>();
  private final List<Models.Unavailability> unavailabilities = new ArrayList<>();
  private final List<Models.RecurringUnavailability> recurring = new ArrayList<>();

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
    recurring.clear();

    var a1 = new Models.Agency(UUID.randomUUID().toString(), "Agence Nord");
    var a2 = new Models.Agency(UUID.randomUUID().toString(), "Agence Sud");
    agencies.add(a1);
    agencies.add(a2);

    clients.add(new Models.Client(UUID.randomUUID().toString(), "Client Alpha", "alpha@acme.tld"));
    clients.add(new Models.Client(UUID.randomUUID().toString(), "Client Beta", "beta@acme.tld"));

    resources.add(
        new Models.Resource(
            UUID.randomUUID().toString(),
            "Grue X",
            "AB-123-CD",
            0xFF4444,
            a1.id(),
            "grue,90t",
            90));
    resources.add(
        new Models.Resource(
            UUID.randomUUID().toString(),
            "Camion Y",
            "EF-456-GH",
            0x44AA44,
            a1.id(),
            "camion,benne",
            18));
    resources.add(
        new Models.Resource(
            UUID.randomUUID().toString(),
            "Remorque Z",
            "IJ-789-KL",
            0x4444FF,
            a2.id(),
            "remorque,plateau",
            10));

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
        clients.get(1).id(),
        "Transport matériel",
        base.plusDays(2).toInstant(),
        base.plusDays(2).plusHours(1).toInstant());

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
    for (Models.Unavailability u : unavailabilities) {
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

  @Override
  public Models.Unavailability createUnavailability(Models.Unavailability unavailability) {
    boolean overlapUnavailability =
        listUnavailabilities(
                unavailability.start().atOffset(ZoneOffset.UTC),
                unavailability.end().atOffset(ZoneOffset.UTC),
                unavailability.resourceId())
            .stream()
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
            unavailability.end(),
            false);
    unavailabilities.add(created);
    return created;
  }

  @Override
  public List<Models.RecurringUnavailability> listRecurringUnavailabilities(String resourceId) {
    return recurring.stream()
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
  public Path downloadResourcesCsv(String tags, Path target) {
    try {
      List<Models.Resource> data = new ArrayList<>(resources);
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

  private static boolean overlaps(Instant start, Instant end, Instant from, Instant to) {
    if (from == null || to == null) {
      return true;
    }
    return end.isAfter(from) && start.isBefore(to);
  }

  private void addIntervention(
      String agencyId, String resourceId, String clientId, String title, Instant start, Instant end) {
    interventions.add(
        new Models.Intervention(
            UUID.randomUUID().toString(), agencyId, resourceId, clientId, title, start, end));
  }

  private void addUnavailability(String resourceId, String reason, Instant start, Instant end) {
    unavailabilities.add(
        new Models.Unavailability(
            UUID.randomUUID().toString(), resourceId, reason, start, end, false));
  }

  @Override
  public void close() {}
}

