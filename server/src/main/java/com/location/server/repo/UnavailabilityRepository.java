package com.location.server.repo;

import com.location.server.domain.Unavailability;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnavailabilityRepository extends JpaRepository<Unavailability, String> {
  @Query("select u from Unavailability u where (:rid is null or u.resource.id = :rid) " +
      "and (:from is null or u.end > :from) and (:to is null or u.start < :to)")
  List<Unavailability> search(@Param("from") OffsetDateTime from,
                              @Param("to") OffsetDateTime to,
                              @Param("rid") String resourceId);

  @Query("select count(u)>0 from Unavailability u where u.resource.id=:rid and u.end > :start and u.start < :end")
  boolean existsOverlap(@Param("rid") String resourceId,
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end);
}
