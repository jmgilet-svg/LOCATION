package com.location.server.repo;

import com.location.server.domain.CommercialDocument;
import com.location.server.domain.DocumentSequence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, String> {
  Optional<DocumentSequence> findByAgencyIdAndYearAndType(
      String agencyId, int year, CommercialDocument.DocType type);
}
