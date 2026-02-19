package com.cloudpulse.service;

import com.cloudpulse.dto.DashboardSummary;
import com.cloudpulse.model.Resource.ResourceStatus;
import com.cloudpulse.repository.HealthCheckRepository;
import com.cloudpulse.repository.IncidentRepository;
import com.cloudpulse.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregation service — computes the executive dashboard summary
 * by pulling data from all three repositories.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ResourceRepository resourceRepository;
    private final HealthCheckRepository healthCheckRepository;
    private final IncidentRepository incidentRepository;

    public DashboardSummary getDashboardSummary() {
        long totalResources = resourceRepository.count();
        long unhealthyCount = resourceRepository.countUnhealthyResources();
        long healthyCount = resourceRepository.findByStatus(ResourceStatus.HEALTHY).size();
        long degradedCount = resourceRepository.findByStatus(ResourceStatus.DEGRADED).size();

        double healthPercent = totalResources > 0
                ? (double) healthyCount / totalResources * 100
                : 0.0;

        LocalDateTime last24h = LocalDateTime.now().minusHours(24);

        return DashboardSummary.builder()
                .totalResources(totalResources)
                .healthyResources(healthyCount)
                .unhealthyResources(unhealthyCount)
                .degradedResources(degradedCount)
                .overallHealthPercent(Math.round(healthPercent * 100.0) / 100.0)
                .activeIncidents(incidentRepository.findActiveIncidents().size())
                .criticalIncidents(incidentRepository.findActiveCriticalIncidents().size())
                .mttrMinutes(incidentRepository.avgResolutionTimeMinutesSince(last24h))
                .healthChecksLast24h(healthCheckRepository.findRecentChecks(last24h).size())
                .avgResponseTimeMs(null) // Would calculate across all resources
                .resourcesByType(toMap(resourceRepository.countByResourceType()))
                .resourcesByRegion(toMap(resourceRepository.countByRegion()))
                .resourcesByStatus(toMap(resourceRepository.countByStatus()))
                .incidentsBySeverity(toMap(incidentRepository.countActiveIncidentsBySeverity()))
                .build();
    }

    private Map<String, Long> toMap(java.util.List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
    }
}
