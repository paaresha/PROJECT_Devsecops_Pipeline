package com.cloudpulse.controller;

import com.cloudpulse.dto.IncidentRequest;
import com.cloudpulse.model.Incident;
import com.cloudpulse.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for incident management.
 *
 * Endpoints:
 * GET /api/incidents — List all incidents
 * GET /api/incidents/active — Active (unresolved) incidents
 * GET /api/incidents/critical — Active critical incidents
 * GET /api/incidents/{id} — Get incident details
 * POST /api/incidents — Create a new incident
 * PUT /api/incidents/{id} — Update an incident
 * POST /api/incidents/{id}/ack — Acknowledge an incident
 * POST /api/incidents/{id}/resolve — Resolve an incident
 * DELETE /api/incidents/{id} — Delete an incident
 */
@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    public ResponseEntity<List<Incident>> getAllIncidents() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Incident>> getActiveIncidents() {
        return ResponseEntity.ok(incidentService.getActiveIncidents());
    }

    @GetMapping("/critical")
    public ResponseEntity<List<Incident>> getCriticalIncidents() {
        return ResponseEntity.ok(incidentService.getActiveCriticalIncidents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getIncidentById(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<List<Incident>> getIncidentsByResource(@PathVariable Long resourceId) {
        return ResponseEntity.ok(incidentService.getIncidentsByResource(resourceId));
    }

    @PostMapping
    public ResponseEntity<Incident> createIncident(@Valid @RequestBody IncidentRequest request) {
        Incident created = incidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Incident> updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody IncidentRequest request) {
        return ResponseEntity.ok(incidentService.updateIncident(id, request));
    }

    @PostMapping("/{id}/ack")
    public ResponseEntity<Incident> acknowledgeIncident(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.acknowledgeIncident(id));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Incident> resolveIncident(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String rootCause = body.getOrDefault("rootCause", "");
        String resolution = body.getOrDefault("resolution", "");
        return ResponseEntity.ok(incidentService.resolveIncident(id, rootCause, resolution));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(@PathVariable Long id) {
        incidentService.deleteIncident(id);
        return ResponseEntity.noContent().build();
    }
}
