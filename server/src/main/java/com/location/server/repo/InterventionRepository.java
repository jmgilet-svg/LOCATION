package com.location.server.repo;

import com.location.server.domain.Intervention;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterventionRepository extends JpaRepository<Intervention, String> {
  @Query(
      "select i from Intervention i where (:rid is null or i.resource.id = :rid) "
          + "and (:from is null or i.end > :from) and (:to is null or i.start < :to)")
  List<Intervention> search(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("rid") String resourceId);

  @Query(
      "select count(i) > 0 from Intervention i where i.resource.id = :rid and i.end > :start and i.start < :end")
  boolean existsOverlap(
      @Param("rid") String resourceId,
      @Param("start") OffsetDateTime start,
      @Param("end") OffsetDateTime end);
}
