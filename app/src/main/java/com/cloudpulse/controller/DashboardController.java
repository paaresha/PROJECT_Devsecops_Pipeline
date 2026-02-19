package com.cloudpulse.controller;

import com.cloudpulse.dto.DashboardSummary;
import com.cloudpulse.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard API — provides the executive overview of infrastructure health.
 *
 * Endpoints:
 * GET /api/dashboard — Returns aggregated health metrics
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardSummary> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }
}
