package com.location.server.repo;

import com.location.server.domain.CommercialDocumentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommercialDocumentLineRepository
    extends JpaRepository<CommercialDocumentLine, String> {
  @Modifying
  @Query("delete from CommercialDocumentLine l where l.document.id = :documentId")
  void deleteByDocumentId(@Param("documentId") String documentId);
}
