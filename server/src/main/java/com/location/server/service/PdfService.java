package com.location.server.service;

import com.location.server.domain.Client;
import com.location.server.domain.Intervention;
import com.location.server.domain.Resource;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PdfService {
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);

  public byte[] buildInterventionPdf(Intervention intervention) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      Document document = new Document(PageSize.A4, 36, 36, 36, 36);
      PdfWriter.getInstance(document, output);
      document.open();

      Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
      Font labelFont = new Font(Font.HELVETICA, 11, Font.BOLD);
      Font valueFont = new Font(Font.HELVETICA, 11);
      Font footerFont = new Font(Font.HELVETICA, 9, Font.ITALIC);

      Paragraph title =
          new Paragraph("Intervention — " + safe(intervention.getTitle()), titleFont);
      title.setSpacingAfter(16f);
      document.add(title);

      PdfPTable table = new PdfPTable(2);
      table.setWidthPercentage(100f);
      table.setSpacingAfter(20f);

      addRow(table, "Agence", intervention.getAgency().getName(), labelFont, valueFont);
      addRow(table, "Client", buildClient(intervention.getClient()), labelFont, valueFont);
      addRow(table, "Ressource", buildResource(intervention.getResource()), labelFont, valueFont);
      addRow(
          table,
          "Début",
          intervention.getStart().format(DATE_TIME_FORMATTER),
          labelFont,
          valueFont);
      addRow(
          table,
          "Fin",
          intervention.getEnd().format(DATE_TIME_FORMATTER),
          labelFont,
          valueFont);

      document.add(table);

      Paragraph footer = new Paragraph("Généré par LOCATION", footerFont);
      footer.setAlignment(Element.ALIGN_RIGHT);
      document.add(footer);

      document.close();
      return output.toByteArray();
    } catch (DocumentException ex) {
      throw new IllegalStateException("Erreur génération PDF", ex);
    }
  }

  private static void addRow(PdfPTable table, String key, String value, Font label, Font font) {
    PdfPCell left = new PdfPCell(new Phrase(key, label));
    PdfPCell right = new PdfPCell(new Phrase(safe(value), font));
    left.setBorderWidth(0.5f);
    right.setBorderWidth(0.5f);
    table.addCell(left);
    table.addCell(right);
  }

  private static String buildClient(Client client) {
    if (client == null) {
      return "";
    }
    StringBuilder builder = new StringBuilder(safe(client.getName()));
    if (client.getBillingEmail() != null && !client.getBillingEmail().isBlank()) {
      builder.append(" — ").append(client.getBillingEmail());
    }
    return builder.toString();
  }

  private static String buildResource(Resource resource) {
    if (resource == null) {
      return "";
    }
    StringBuilder builder = new StringBuilder(safe(resource.getName()));
    if (resource.getLicensePlate() != null && !resource.getLicensePlate().isBlank()) {
      builder.append(" (").append(resource.getLicensePlate()).append(")");
    }
    if (resource.getCapacityTons() != null) {
      builder.append(" - ").append(resource.getCapacityTons()).append(" t");
    }
    return builder.toString();
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }
}
