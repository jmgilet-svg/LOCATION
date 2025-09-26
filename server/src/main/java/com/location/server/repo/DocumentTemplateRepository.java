package com.location.server.repo;

import com.location.server.domain.CommercialDocument;
import com.location.server.domain.DocumentTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, String> {
  Optional<DocumentTemplate> findByAgencyIdAndDocumentType(
      String agencyId, CommercialDocument.DocType documentType);
}
