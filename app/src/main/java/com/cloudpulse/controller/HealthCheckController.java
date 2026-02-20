package com.cloudpulse.controller;

import com.cloudpulse.model.HealthCheck;
import com.cloudpulse.model.Resource;
import com.cloudpulse.service.HealthCheckService;
import com.cloudpulse.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for health check operations.
 *
 * Endpoints:
 * GET /api/healthchecks/resource/{id} — Get check history for a resource
 * GET /api/healthchecks/recent?hours=24 — Get recent checks across all
 * resources
 * POST /api/healthchecks/resource/{id}/run — Trigger an on-demand health check
 * GET /api/healthchecks/resource/{id}/avg-time — Get avg response time
 */
@RestController
@RequestMapping("/healthchecks")
@RequiredArgsConstructor
public class HealthCheckController {

    private final HealthCheckService healthCheckService;
    private final ResourceService resourceService;

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<List<HealthCheck>> getCheckHistory(
            @PathVariable Long resourceId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(healthCheckService.getChecksForResource(resourceId, limit));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<HealthCheck>> getRecentChecks(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(healthCheckService.getRecentChecks(hours));
    }

    @PostMapping("/resource/{resourceId}/run")
    public ResponseEntity<HealthCheck> runHealthCheck(@PathVariable Long resourceId) {
        Resource resource = resourceService.getResourceById(resourceId);
        HealthCheck result = healthCheckService.performHealthCheck(resource);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/resource/{resourceId}/avg-time")
    public ResponseEntity<Map<String, Object>> getAvgResponseTime(
            @PathVariable Long resourceId,
            @RequestParam(defaultValue = "24") int hours) {
        Double avgTime = healthCheckService.getAvgResponseTime(resourceId, hours);
        return ResponseEntity.ok(Map.of(
                "resourceId", resourceId,
                "avgResponseTimeMs", avgTime != null ? avgTime : 0,
                "periodHours", hours));
    }
}
