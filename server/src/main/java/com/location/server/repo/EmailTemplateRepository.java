package com.location.server.repo;

import com.location.server.domain.CommercialDocument;
import com.location.server.domain.EmailTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, String> {
  Optional<EmailTemplate> findByAgencyIdAndDocumentType(
      String agencyId, CommercialDocument.DocType documentType);
}
