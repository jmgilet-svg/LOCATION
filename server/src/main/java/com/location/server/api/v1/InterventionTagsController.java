package com.location.server.api.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interventions")
public class InterventionTagsController {

  private static final Map<String, List<String>> tagsByIntervention = new ConcurrentHashMap<>();

  @GetMapping(value = "/{id}/tags", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<String>> getTags(@PathVariable("id") String id) {
    if (id == null || id.isBlank()) {
      return ResponseEntity.ok(List.of());
    }
    return ResponseEntity.ok(tagsByIntervention.getOrDefault(id, List.of()));
  }

  @PostMapping(value = "/{id}/tags", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> setTags(
      @PathVariable("id") String id, @RequestBody(required = false) List<String> tags) {
    if (id == null || id.isBlank()) {
      return ResponseEntity.ok().build();
    }
    tagsByIntervention.put(id, tags == null ? List.of() : new ArrayList<>(tags));
    return ResponseEntity.ok().build();
  }

  @GetMapping(value = "/tags/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<String>> suggestTags(
      @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
    Map<String, Integer> frequency = new HashMap<>();
    for (List<String> tags : tagsByIntervention.values()) {
      for (String tag : tags) {
        if (tag == null) {
          continue;
        }
        String normalized = tag.trim();
        if (normalized.isEmpty()) {
          continue;
        }
        frequency.merge(normalized, 1, Integer::sum);
      }
    }

    List<String> sorted = new ArrayList<>(frequency.keySet());
    sorted.sort((a, b) -> Integer.compare(frequency.getOrDefault(b, 0), frequency.getOrDefault(a, 0)));

    int effectiveLimit = Math.max(1, limit);
    if (sorted.size() > effectiveLimit) {
      sorted = sorted.subList(0, effectiveLimit);
    }
    return ResponseEntity.ok(sorted);
  }
}
