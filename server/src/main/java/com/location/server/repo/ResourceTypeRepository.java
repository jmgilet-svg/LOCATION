package com.location.server.repo;

import com.location.server.domain.ResourceType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceTypeRepository extends JpaRepository<ResourceType, String> {

  Optional<ResourceType> findByNameIgnoreCase(String name);
}
