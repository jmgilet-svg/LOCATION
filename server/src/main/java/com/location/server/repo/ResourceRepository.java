package com.location.server.repo;

import com.location.server.domain.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, String> {

  default List<Resource> searchByTags(String tagsCsv) {
    if (tagsCsv == null || tagsCsv.isBlank()) {
      return findAll();
    }
    List<String> requested =
        Arrays.stream(tagsCsv.toLowerCase().split("\\s*,\\s*")).filter(s -> !s.isBlank()).collect(Collectors.toList());
    if (requested.isEmpty()) {
      return findAll();
    }
    return findAll().stream()
        .filter(resource -> resource.getTags() != null)
        .filter(
            resource -> {
              String lower = resource.getTags().toLowerCase();
              for (String tag : requested) {
                if (!lower.contains(tag)) {
                  return false;
                }
              }
              return true;
            })
        .collect(Collectors.toList());
  }
}
