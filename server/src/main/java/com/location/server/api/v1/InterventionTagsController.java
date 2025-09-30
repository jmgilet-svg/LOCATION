package com.location.server.api.v1;

import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interventions")
public class InterventionTagsController {

  private final Map<String, List<String>> tagsByIntervention = new ConcurrentHashMap<>();

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
}
