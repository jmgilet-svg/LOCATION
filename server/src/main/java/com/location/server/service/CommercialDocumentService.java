package com.location.server.service;

import com.location.server.domain.Agency;
import com.location.server.domain.Client;
import com.location.server.domain.CommercialDocument;
import com.location.server.domain.CommercialDocument.DocStatus;
import com.location.server.domain.CommercialDocument.DocType;
import com.location.server.domain.CommercialDocumentLine;
import com.location.server.repo.AgencyRepository;
import com.location.server.repo.ClientRepository;
import com.location.server.repo.CommercialDocumentLineRepository;
import com.location.server.repo.CommercialDocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommercialDocumentService {
  private final CommercialDocumentRepository documentRepository;
  private final CommercialDocumentLineRepository lineRepository;
  private final AgencyRepository agencyRepository;
  private final ClientRepository clientRepository;

  public CommercialDocumentService(
      CommercialDocumentRepository documentRepository,
      CommercialDocumentLineRepository lineRepository,
      AgencyRepository agencyRepository,
      ClientRepository clientRepository) {
    this.documentRepository = documentRepository;
    this.lineRepository = lineRepository;
    this.agencyRepository = agencyRepository;
    this.clientRepository = clientRepository;
  }

  @Transactional
  public CommercialDocument create(DocType type, String agencyId, String clientId, String title) {
    Agency agency = agencyRepository.findById(agencyId).orElseThrow(() -> notFound("agency", agencyId));
    Client client = clientRepository.findById(clientId).orElseThrow(() -> notFound("client", clientId));
    CommercialDocument document =
        new CommercialDocument(
            UUID.randomUUID().toString(),
            type,
            DocStatus.DRAFT,
            null,
            title,
            OffsetDateTime.now(),
            agency,
            client);
    return documentRepository.save(document);
  }

  @Transactional
  public CommercialDocument update(
      String id, String reference, String title, List<LinePayload> linesPayload) {
    CommercialDocument document =
        documentRepository.findById(id).orElseThrow(() -> notFound("document", id));
    document.setReference(reference != null && reference.isBlank() ? null : reference);
    document.setTitle(title);

    document.getLines().clear();
    lineRepository.deleteByDocumentId(document.getId());

    List<CommercialDocumentLine> newLines = new ArrayList<>();
    int index = 1;
    for (LinePayload payload : linesPayload) {
      CommercialDocumentLine line =
          new CommercialDocumentLine(
              UUID.randomUUID().toString(),
              document,
              index++,
              payload.designation(),
              scaled(payload.quantity(), 3),
              scaled(payload.unitPrice(), 2),
              scaled(payload.vatRate(), 2));
      newLines.add(line);
    }
    document.getLines().addAll(newLines);
    recomputeTotals(document);
    return documentRepository.save(document);
  }

  @Transactional
  public CommercialDocument transition(String id, DocType toType) {
    CommercialDocument source =
        documentRepository.findById(id).orElseThrow(() -> notFound("document", id));
    CommercialDocument copy =
        new CommercialDocument(
            UUID.randomUUID().toString(),
            toType,
            toType == DocType.INVOICE ? DocStatus.ISSUED : DocStatus.DRAFT,
            null,
            source.getTitle(),
            OffsetDateTime.now(),
            source.getAgency(),
            source.getClient());
    documentRepository.save(copy);

    int index = 1;
    for (CommercialDocumentLine line : source.getLines()) {
      CommercialDocumentLine clone =
          new CommercialDocumentLine(
              UUID.randomUUID().toString(),
              copy,
              index++,
              line.getDesignation(),
              line.getQuantity(),
              line.getUnitPrice(),
              line.getVatRate());
      copy.getLines().add(clone);
    }
    recomputeTotals(copy);
    return documentRepository.save(copy);
  }

  private void recomputeTotals(CommercialDocument document) {
    BigDecimal totalHt = BigDecimal.ZERO;
    BigDecimal totalVat = BigDecimal.ZERO;
    for (CommercialDocumentLine line : document.getLines()) {
      BigDecimal lineHt =
          line.getUnitPrice().multiply(line.getQuantity()).setScale(2, RoundingMode.HALF_UP);
      BigDecimal lineVat =
          lineHt.multiply(line.getVatRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
      totalHt = totalHt.add(lineHt);
      totalVat = totalVat.add(lineVat);
    }
    document.setTotals(totalHt, totalVat, totalHt.add(totalVat));
  }

  private static BigDecimal scaled(double value, int scale) {
    return new BigDecimal(String.valueOf(value)).setScale(scale, RoundingMode.HALF_UP);
  }

  private static EntityNotFoundException notFound(String entity, String id) {
    return new EntityNotFoundException(entity + " " + id + " not found");
  }

  public record LinePayload(String designation, double quantity, double unitPrice, double vatRate) {}
}
