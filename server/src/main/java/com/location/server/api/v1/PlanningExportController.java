package com.location.server.api.v1;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/planning")
public class PlanningExportController {

  private static final DateTimeFormatter FOOTER_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> exportPdf(
      @RequestPart("image") MultipartFile image,
      @RequestParam(value = "title", required = false) String title)
      throws IOException {
    if (image == null || image.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    String safeTitle = title == null || title.isBlank() ? "Planning" : title.trim();

    byte[] pdfBytes;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (PdfWriter writer = new PdfWriter(baos);
          PdfDocument pdf = new PdfDocument(writer);
          Document doc = new Document(pdf, PageSize.A4)) {
        doc.setMargins(20, 20, 30, 20);

        Image img = new Image(ImageDataFactory.create(image.getBytes()));

        float maxWidth =
            doc.getPdfDocument().getDefaultPageSize().getWidth()
                - doc.getLeftMargin()
                - doc.getRightMargin();
        float maxHeight =
            doc.getPdfDocument().getDefaultPageSize().getHeight()
                - doc.getTopMargin()
                - doc.getBottomMargin();
        float imageWidth = img.getImageWidth();
        float imageHeight = img.getImageHeight();
        if (imageWidth <= 0 || imageHeight <= 0) {
          imageWidth = maxWidth;
          imageHeight = maxHeight;
        }
        float scale = Math.min(maxWidth / imageWidth, maxHeight / imageHeight);
        img.scale(scale, scale);
        img.setHorizontalAlignment(HorizontalAlignment.CENTER);
        doc.add(img);

        String footer =
            safeTitle + " — généré le " + LocalDateTime.now().format(FOOTER_FORMATTER);
        Paragraph paragraph =
            new Paragraph(footer).setFontSize(8).setTextAlignment(TextAlignment.CENTER);
        doc.add(paragraph);
      }
      pdfBytes = baos.toByteArray();
    }

    String normalized = safeTitle.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    normalized = normalized.replaceAll("^-+", "").replaceAll("-+$", "");
    if (normalized.isBlank()) {
      normalized = "planning";
    }
    String filename = normalized + ".pdf";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdfBytes);
  }
}
