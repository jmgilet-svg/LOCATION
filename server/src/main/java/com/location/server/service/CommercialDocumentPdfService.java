package com.location.server.service;

import com.location.server.domain.Agency;
import com.location.server.domain.CommercialDocument;
import com.location.server.domain.CommercialDocumentLine;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CommercialDocumentPdfService {
  public byte[] build(CommercialDocument document) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Document pdf = new Document(PageSize.A4, 36, 36, 36, 36);
      PdfWriter.getInstance(pdf, baos);
      pdf.open();

      Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
      Font normal = new Font(Font.HELVETICA, 11);
      addHeader(pdf, document, titleFont);
      Paragraph title = new Paragraph(titleFor(document), titleFont);
      title.setSpacingAfter(8f);
      pdf.add(title);

      PdfPTable metadata = new PdfPTable(2);
      metadata.setWidthPercentage(100);
      addRow(metadata, "Agence", document.getAgency().getName(), normal);
      addRow(metadata, "Client", document.getClient().getName(), normal);
      if (hasText(document.getClient().getBillingAddress())
          || hasText(document.getClient().getBillingZip())
          || hasText(document.getClient().getBillingCity())) {
        addRow(metadata, "Adresse client", formatClientAddress(document), normal);
      }
      if (hasText(document.getClient().getVatNumber())) {
        addRow(metadata, "TVA client", document.getClient().getVatNumber(), normal);
      }
      if (hasText(document.getClient().getIban())) {
        addRow(metadata, "IBAN", document.getClient().getIban(), normal);
      }
      addRow(metadata, "Référence", document.getReference() == null ? "" : document.getReference(), normal);
      addRow(metadata, "Date", document.getDate().toLocalDate().toString(), normal);
      pdf.add(metadata);

      pdf.add(new Paragraph(" "));

      PdfPTable lines = new PdfPTable(new float[] {5f, 1.5f, 2f, 1.5f, 2f});
      lines.setWidthPercentage(100);
      header(lines, "Désignation", "Qté", "PU", "TVA %", "Total HT");

      NumberFormat number = NumberFormat.getNumberInstance(Locale.FRANCE);
      number.setMinimumFractionDigits(2);
      number.setMaximumFractionDigits(2);
      for (CommercialDocumentLine line : document.getLines()) {
        var lineHt = line.getUnitPrice().multiply(line.getQuantity());
        addRow(
            lines,
            line.getDesignation(),
            number.format(line.getQuantity()),
            number.format(line.getUnitPrice()),
            number.format(line.getVatRate()),
            number.format(lineHt));
      }
      pdf.add(lines);

      pdf.add(new Paragraph(" "));

      PdfPTable totals = new PdfPTable(new float[] {3f, 2f});
      totals.setWidthPercentage(60);
      totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
      addRow(totals, "Total HT", number.format(document.getTotalHt()), normal);
      addRow(totals, "TVA", number.format(document.getTotalVat()), normal);
      addRow(totals, "Total TTC", number.format(document.getTotalTtc()), normal);
      pdf.add(totals);

      addAgencyFooter(pdf, document.getAgency());

      pdf.close();
      return baos.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException("Unable to generate document PDF", e);
    }
  }

  private void addHeader(Document pdf, CommercialDocument document, Font brandFont)
      throws Exception {
    String brand =
        hasText(document.getAgency().getName())
            ? document.getAgency().getName()
            : System.getenv().getOrDefault("AGENCY_NAME", "LOCATION");
    Image logo = loadLogo(document.getAgency().getLogoPng());
    PdfPTable header = new PdfPTable(logo != null ? 2 : 1);
    header.setWidthPercentage(100);
    header.getDefaultCell().setBorder(0);
    if (logo != null) {
      PdfPCell logoCell = new PdfPCell(logo);
      logoCell.setBorder(0);
      logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
      header.addCell(logoCell);
    }
    PdfPCell brandCell = new PdfPCell(new Phrase(brand, brandFont));
    brandCell.setBorder(0);
    brandCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    brandCell.setHorizontalAlignment(Element.ALIGN_LEFT);
    header.addCell(brandCell);
    pdf.add(header);
  }

  private static String titleFor(CommercialDocument document) {
    String base =
        switch (document.getType()) {
          case QUOTE -> "Devis";
          case ORDER -> "Commande";
          case DELIVERY -> "Bon de livraison";
          case INVOICE -> "Facture";
        };
    return document.getReference() == null || document.getReference().isBlank()
        ? base
        : base + " " + document.getReference();
  }

  private static void header(PdfPTable table, String... columns) {
    Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
    for (String column : columns) {
      PdfPCell cell = new PdfPCell(new Phrase(column, headerFont));
      cell.setGrayFill(0.9f);
      table.addCell(cell);
    }
  }

  private static void addRow(PdfPTable table, String... values) {
    Font font = new Font(Font.HELVETICA, 10);
    for (String value : values) {
      table.addCell(new Phrase(value, font));
    }
  }

  private static void addRow(PdfPTable table, String key, String value, Font font) {
    PdfPCell k = new PdfPCell(new Phrase(key, font));
    k.setBorderWidth(0.5f);
    PdfPCell v = new PdfPCell(new Phrase(value, font));
    v.setBorderWidth(0.5f);
    table.addCell(k);
    table.addCell(v);
  }

  private void addAgencyFooter(Document pdf, Agency agency)
      throws Exception {
    Font footerFont = new Font(Font.HELVETICA, 9);
    if (hasText(agency.getLegalFooter())) {
      Paragraph footer = new Paragraph(agency.getLegalFooter(), footerFont);
      footer.setSpacingBefore(12f);
      pdf.add(footer);
    }
    if (hasText(agency.getIban())) {
      Paragraph iban = new Paragraph("IBAN : " + agency.getIban(), footerFont);
      iban.setSpacingBefore(hasText(agency.getLegalFooter()) ? 4f : 12f);
      pdf.add(iban);
    }
  }

  private Image loadLogo(byte[] custom) {
    try {
      if (custom != null && custom.length > 0) {
        return buildLogo(custom);
      }
      try (InputStream input = getClass().getResourceAsStream("/static/logo.png")) {
        if (input == null) {
          return null;
        }
        return buildLogo(input.readAllBytes());
      }
    } catch (Exception e) {
      return null;
    }
  }

  private Image buildLogo(byte[] bytes) throws Exception {
    Image image = Image.getInstance(bytes);
    image.scaleToFit(120f, 60f);
    image.setAlignment(Element.ALIGN_LEFT);
    return image;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static String formatClientAddress(CommercialDocument document) {
    String address = document.getClient().getBillingAddress();
    String zip = document.getClient().getBillingZip();
    String city = document.getClient().getBillingCity();
    return joinNonBlank(
        ", ",
        address,
        joinNonBlank(" ", zip, city));
  }

  private static String joinNonBlank(String separator, String... parts) {
    StringBuilder builder = new StringBuilder();
    for (String part : parts) {
      if (part != null && !part.isBlank()) {
        if (!builder.isEmpty()) {
          builder.append(separator);
        }
        builder.append(part.trim());
      }
    }
    return builder.toString();
  }
}
