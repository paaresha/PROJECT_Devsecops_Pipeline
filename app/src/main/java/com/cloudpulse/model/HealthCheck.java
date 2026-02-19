package com.cloudpulse.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records the result of a health check performed on a monitored resource.
 * Each resource can have many health checks over time, forming a timeline.
 */
@Entity
@Table(name = "health_checks", indexes = {
        @Index(name = "idx_hc_resource", columnList = "resource_id"),
        @Index(name = "idx_hc_status", columnList = "status"),
        @Index(name = "idx_hc_checked_at", columnList = "checkedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HealthStatus status;

    private Integer responseTimeMs; // Latency in milliseconds

    private Integer statusCode; // HTTP status code (for web endpoints)

    @Column(columnDefinition = "TEXT")
    private String message; // Human-readable status message

    @Column(columnDefinition = "TEXT")
    private String details; // JSON with detailed check results

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime checkedAt;

    public enum HealthStatus {
        UP, DOWN, DEGRADED, TIMEOUT, UNREACHABLE
    }
}
