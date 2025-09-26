package com.location.server.repo;

import com.location.server.domain.CommercialDocument;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommercialDocumentRepository extends JpaRepository<CommercialDocument, String> {
  @Query(
      "select d from CommercialDocument d "
          + "where (:type is null or d.type = :type) "
          + "and (:clientId is null or d.client.id = :clientId) "
          + "and (:from is null or d.date >= :from) "
          + "and (:to is null or d.date < :to) "
          + "order by d.date desc")
  List<CommercialDocument> search(
      @Param("type") CommercialDocument.DocType type,
      @Param("clientId") String clientId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);
}
