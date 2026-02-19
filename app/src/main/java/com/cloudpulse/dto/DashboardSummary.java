package com.cloudpulse.dto;

import lombok.*;
import java.util.Map;

/**
 * Dashboard summary DTO — provides a high-level overview of infrastructure
 * health.
 * This is the "executive view" returned by GET /api/dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummary {

    private long totalResources;
    private long healthyResources;
    private long unhealthyResources;
    private long degradedResources;
    private double overallHealthPercent;

    private long activeIncidents;
    private long criticalIncidents;
    private Double mttrMinutes; // Mean Time To Resolution

    private long healthChecksLast24h;
    private Double avgResponseTimeMs;

    private Map<String, Long> resourcesByType;
    private Map<String, Long> resourcesByRegion;
    private Map<String, Long> resourcesByStatus;
    private Map<String, Long> incidentsBySeverity;
}
