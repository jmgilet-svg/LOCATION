package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CsvUtil {
  private CsvUtil() {}

  public static void exportPlanningDay(DataSourceProvider dsp, LocalDate day, Path target)
      throws IOException {
    ZoneId zone = ZoneId.systemDefault();
    OffsetDateTime from = day.atStartOfDay(zone).toOffsetDateTime();
    OffsetDateTime to = day.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
    List<Models.Intervention> interventions = dsp.listInterventions(from, to, null);
    DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
      writer.write("resourceId;start;end;title;clientId\n");
      for (Models.Intervention intervention : interventions) {
        OffsetDateTime start = OffsetDateTime.ofInstant(intervention.start(), zone);
        OffsetDateTime end = OffsetDateTime.ofInstant(intervention.end(), zone);
        writer.write(
            escape(intervention.resourceId())
                + ';'
                + start.format(fmt)
                + ';'
                + end.format(fmt)
                + ';'
                + escape(intervention.title())
                + ';'
                + escape(intervention.clientId()));
        writer.newLine();
      }
    }
  }

  public static void exportClients(DataSourceProvider dsp, Path target) throws IOException {
    List<Models.Client> clients = dsp.listClients();
    try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
      writer.write("id;name;email;address;zip;city;vatNumber;iban\n");
      for (Models.Client client : clients) {
        writer.write(
            escape(client.id())
                + ';'
                + escape(client.name())
                + ';'
                + escape(client.billingEmail())
                + ';'
                + escape(client.billingAddress())
                + ';'
                + escape(client.billingZip())
                + ';'
                + escape(client.billingCity())
                + ';'
                + escape(client.vatNumber())
                + ';'
                + escape(client.iban()));
        writer.newLine();
      }
    }
  }

  private static String escape(String value) {
    if (value == null) {
      return "";
    }
    String normalized = value.replace("\"", "\"\"");
    if (normalized.contains(";") || normalized.contains("\"") || normalized.contains("\n")) {
      return '"' + normalized + '"';
    }
    return normalized;
  }
}
