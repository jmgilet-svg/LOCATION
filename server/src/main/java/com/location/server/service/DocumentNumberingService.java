package com.location.server.service;

import com.location.server.domain.Agency;
import com.location.server.domain.CommercialDocument;
import com.location.server.domain.DocumentSequence;
import com.location.server.repo.DocumentSequenceRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentNumberingService {

  private static final Map<CommercialDocument.DocType, String> PREFIXES =
      Map.of(
          CommercialDocument.DocType.QUOTE, "DV",
          CommercialDocument.DocType.ORDER, "BC",
          CommercialDocument.DocType.DELIVERY, "BL",
          CommercialDocument.DocType.INVOICE, "FA");

  private final DocumentSequenceRepository repository;

  public DocumentNumberingService(DocumentSequenceRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public String nextReference(Agency agency, CommercialDocument.DocType type, OffsetDateTime date) {
    int year = date.getYear();
    DocumentSequence sequence =
        repository
            .findByAgencyIdAndYearAndType(agency.getId(), year, type)
            .orElseGet(
                () ->
                    repository.save(
                        new DocumentSequence(
                            UUID.randomUUID().toString(), agency, year, type, /*lastNumber=*/ 0)));
    int next = sequence.getLastNumber() + 1;
    sequence.setLastNumber(next);
    repository.save(sequence);
    String prefix = PREFIXES.getOrDefault(type, type.name());
    return "%s-%d-%04d".formatted(prefix, year, next);
  }
}
