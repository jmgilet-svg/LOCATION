package com.location.server.api.v1;

import com.location.server.api.v1.dto.ApiV1Dtos.ResourceTypeAssignmentDto;
import com.location.server.api.v1.dto.ApiV1Dtos.ResourceTypeAssignmentRequest;
import com.location.server.api.v1.dto.ApiV1Dtos.ResourceTypeDto;
import com.location.server.domain.Resource;
import com.location.server.domain.ResourceType;
import com.location.server.repo.ResourceRepository;
import com.location.server.repo.ResourceTypeRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ResourceTypeController {

  private final ResourceTypeRepository resourceTypeRepository;
  private final ResourceRepository resourceRepository;

  public ResourceTypeController(
      ResourceTypeRepository resourceTypeRepository, ResourceRepository resourceRepository) {
    this.resourceTypeRepository = resourceTypeRepository;
    this.resourceRepository = resourceRepository;
  }

  @GetMapping("/resource-types")
  public List<ResourceTypeDto> list() {
    return resourceTypeRepository.findAll().stream()
        .sorted(Comparator.comparing(ResourceType::getName, String.CASE_INSENSITIVE_ORDER))
        .map(ResourceTypeDto::of)
        .toList();
  }

  @PostMapping("/resource-types")
  @Transactional
  public ResponseEntity<ResourceTypeDto> save(@Valid @RequestBody ResourceTypeDto dto) {
    String trimmedName = dto.name().trim();
    String trimmedIcon = dto.iconName().trim();

    ResourceType entity = null;
    if (dto.id() != null && !dto.id().isBlank()) {
      entity = resourceTypeRepository.findById(dto.id()).orElse(null);
    }
    if (entity == null) {
      entity = resourceTypeRepository.findByNameIgnoreCase(trimmedName).orElse(null);
    }
    if (entity == null) {
      entity = new ResourceType(UUID.randomUUID().toString(), trimmedName, trimmedIcon);
    }

    entity.setName(trimmedName);
    entity.setIconName(trimmedIcon);
    ResourceType saved = resourceTypeRepository.save(entity);
    return ResponseEntity.created(URI.create("/api/v1/resource-types/" + saved.getId()))
        .body(ResourceTypeDto.of(saved));
  }

  @DeleteMapping("/resource-types/{id}")
  @Transactional
  public ResponseEntity<Void> delete(@PathVariable String id) {
    ResourceType type = resourceTypeRepository.findById(id).orElse(null);
    if (type == null) {
      return ResponseEntity.noContent().build();
    }
    for (Resource resource : resourceRepository.findByResourceType(type)) {
      resource.setResourceType(null);
      resourceRepository.save(resource);
    }
    resourceTypeRepository.delete(type);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/resources/{id}/type")
  public ResourceTypeAssignmentDto getResourceType(@PathVariable String id) {
    Resource resource =
        resourceRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ressource inconnue"));
    ResourceType type = resource.getResourceType();
    return new ResourceTypeAssignmentDto(type == null ? null : type.getId());
  }

  @PutMapping("/resources/{id}/type")
  @Transactional
  public ResourceTypeAssignmentDto setResourceType(
      @PathVariable String id, @Valid @RequestBody ResourceTypeAssignmentRequest request) {
    Resource resource =
        resourceRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ressource inconnue"));
    ResourceType type =
        resourceTypeRepository
            .findById(request.resourceTypeId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type inconnu"));
    resource.setResourceType(type);
    resourceRepository.save(resource);
    return new ResourceTypeAssignmentDto(type.getId());
  }

  @DeleteMapping("/resources/{id}/type")
  @Transactional
  public ResponseEntity<Void> clearResourceType(@PathVariable String id) {
    Resource resource =
        resourceRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ressource inconnue"));
    resource.setResourceType(null);
    resourceRepository.save(resource);
    return ResponseEntity.noContent().build();
  }
}
