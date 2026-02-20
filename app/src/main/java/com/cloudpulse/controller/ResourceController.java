package com.cloudpulse.controller;

import com.cloudpulse.dto.ResourceRequest;
import com.cloudpulse.model.Resource;
import com.cloudpulse.model.Resource.ResourceStatus;
import com.cloudpulse.model.Resource.ResourceType;
import com.cloudpulse.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for managing monitored cloud resources.
 *
 * Endpoints:
 * GET /api/resources — List all resources
 * GET /api/resources/{id} — Get resource by ID
 * GET /api/resources/unhealthy — List unhealthy/degraded resources
 * GET /api/resources?type=EC2 — Filter by type
 * GET /api/resources?region=... — Filter by region
 * GET /api/resources?provider=aws — Filter by provider
 * POST /api/resources — Register a new resource
 * PUT /api/resources/{id} — Update a resource
 * PATCH /api/resources/{id}/status — Update resource status only
 * DELETE /api/resources/{id} — Remove a resource
 */
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<List<Resource>> getAllResources(
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) ResourceStatus status,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String provider) {

        List<Resource> resources;

        if (type != null) {
            resources = resourceService.getByType(type);
        } else if (status != null) {
            resources = resourceService.getByStatus(status);
        } else if (region != null) {
            resources = resourceService.getByRegion(region);
        } else if (provider != null) {
            resources = resourceService.getByProvider(provider);
        } else {
            resources = resourceService.getAllResources();
        }

        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getResourceById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @GetMapping("/unhealthy")
    public ResponseEntity<List<Resource>> getUnhealthyResources() {
        return ResponseEntity.ok(resourceService.getUnhealthy());
    }

    @PostMapping
    public ResponseEntity<Resource> createResource(@Valid @RequestBody ResourceRequest request) {
        Resource created = resourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Resource> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.updateResource(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Resource> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ResourceStatus status = ResourceStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(resourceService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
