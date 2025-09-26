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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public final class IcsUtil {
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

  private IcsUtil() {}

  public static void exportPlanningDay(DataSourceProvider dsp, LocalDate day, Path target)
      throws IOException {
    OffsetDateTime from = day.atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime to = from.plusDays(1);
    List<Models.Intervention> interventions = dsp.listInterventions(from, to, null);
    try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
      writer.write("BEGIN:VCALENDAR\r\n");
      writer.write("VERSION:2.0\r\n");
      writer.write("PRODID:-//LOCATION//Planning//FR\r\n");
      for (Models.Intervention intervention : interventions) {
        String uid = UUID.randomUUID() + "@location";
        writer.write("BEGIN:VEVENT\r\n");
        writer.write("UID:" + uid + "\r\n");
        writer.write("DTSTART:" + FORMATTER.format(intervention.start().atOffset(ZoneOffset.UTC)) + "\r\n");
        writer.write("DTEND:" + FORMATTER.format(intervention.end().atOffset(ZoneOffset.UTC)) + "\r\n");
        writer.write("SUMMARY:" + escape(intervention.title()) + "\r\n");
        writer.write(
            "DESCRIPTION:Client="
                + escape(intervention.clientId())
                + ";Ressource="
                + escape(intervention.resourceId())
                + "\r\n");
        writer.write("END:VEVENT\r\n");
      }
      writer.write("END:VCALENDAR\r\n");
    }
  }

  private static String escape(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n");
  }
}
