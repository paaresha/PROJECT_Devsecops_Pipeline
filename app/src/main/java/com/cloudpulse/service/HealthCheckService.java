package com.cloudpulse.service;

import com.cloudpulse.model.HealthCheck;
import com.cloudpulse.model.HealthCheck.HealthStatus;
import com.cloudpulse.model.Resource;
import com.cloudpulse.model.Resource.ResourceStatus;
import com.cloudpulse.repository.HealthCheckRepository;
import com.cloudpulse.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final HealthCheckRepository healthCheckRepository;
    private final ResourceRepository resourceRepository;
    private final Random random = new Random();

    /**
     * Returns the latest N health checks for a specific resource.
     */
    public List<HealthCheck> getChecksForResource(Long resourceId, int limit) {
        return healthCheckRepository.findLatestByResourceId(resourceId, PageRequest.of(0, limit));
    }

    /**
     * Returns all health checks from the last N hours.
     */
    public List<HealthCheck> getRecentChecks(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return healthCheckRepository.findRecentChecks(since);
    }

    /**
     * Returns the average response time for a resource over the last N hours.
     */
    public Double getAvgResponseTime(Long resourceId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return healthCheckRepository.avgResponseTimeByResourceSince(resourceId, since);
    }

    /**
     * Performs a health check on a specific resource and records the result.
     * In a real implementation, this would ping the actual resource.
     * For demo purposes, it simulates realistic health check behavior.
     */
    @Transactional
    public HealthCheck performHealthCheck(Resource resource) {
        // Simulate a health check (in production, this would be a real HTTP/TCP probe)
        HealthStatus status = simulateHealthCheck();
        int responseTime = simulateResponseTime(status);
        int statusCode = (status == HealthStatus.UP) ? 200 : (status == HealthStatus.DEGRADED) ? 503 : 0;

        HealthCheck check = HealthCheck.builder()
                .resource(resource)
                .status(status)
                .responseTimeMs(responseTime)
                .statusCode(statusCode)
                .message(buildStatusMessage(status, resource.getName()))
                .build();

        HealthCheck saved = healthCheckRepository.save(check);

        // Update the resource status based on this health check
        ResourceStatus newStatus = mapToResourceStatus(status);
        resource.setStatus(newStatus);
        resource.setLastCheckedAt(LocalDateTime.now());
        resourceRepository.save(resource);

        return saved;
    }

    /**
     * Scheduled job: runs health checks on all monitored resources every 5 minutes.
     */
    @Scheduled(fixedDelayString = "${cloudpulse.healthcheck.interval:300000}")
    @Transactional
    public void scheduledHealthChecks() {
        List<Resource> resources = resourceRepository.findAll();
        if (resources.isEmpty())
            return;

        log.info("Running scheduled health checks on {} resources...", resources.size());
        for (Resource resource : resources) {
            try {
                performHealthCheck(resource);
            } catch (Exception e) {
                log.error("Health check failed for resource {}: {}", resource.getName(), e.getMessage());
            }
        }
        log.info("Scheduled health checks completed");
    }

    // ---- Simulation helpers (replace with real probes in production) ----

    private HealthStatus simulateHealthCheck() {
        int roll = random.nextInt(100);
        if (roll < 80)
            return HealthStatus.UP;
        if (roll < 90)
            return HealthStatus.DEGRADED;
        if (roll < 97)
            return HealthStatus.DOWN;
        return HealthStatus.TIMEOUT;
    }

    private int simulateResponseTime(HealthStatus status) {
        return switch (status) {
            case UP -> 20 + random.nextInt(180); // 20-200ms
            case DEGRADED -> 500 + random.nextInt(2000); // 500-2500ms
            case DOWN -> 0;
            case TIMEOUT -> 30000; // 30s timeout
            case UNREACHABLE -> 0;
        };
    }

    private ResourceStatus mapToResourceStatus(HealthStatus healthStatus) {
        return switch (healthStatus) {
            case UP -> ResourceStatus.HEALTHY;
            case DEGRADED -> ResourceStatus.DEGRADED;
            case DOWN, TIMEOUT, UNREACHABLE -> ResourceStatus.UNHEALTHY;
        };
    }

    private String buildStatusMessage(HealthStatus status, String resourceName) {
        return switch (status) {
            case UP -> resourceName + " is responding normally";
            case DEGRADED -> resourceName + " is experiencing high latency";
            case DOWN -> resourceName + " is not responding";
            case TIMEOUT -> resourceName + " health check timed out";
            case UNREACHABLE -> resourceName + " is unreachable";
        };
    }
}
