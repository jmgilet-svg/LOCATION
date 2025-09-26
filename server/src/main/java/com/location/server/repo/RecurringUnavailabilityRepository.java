package com.location.server.repo;

import com.location.server.domain.RecurringUnavailability;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurringUnavailabilityRepository
    extends JpaRepository<RecurringUnavailability, String> {

  @Query(
      "select r from RecurringUnavailability r where (:rid is null or r.resource.id = :rid)")
  List<RecurringUnavailability> search(@Param("rid") String resourceId);
}

