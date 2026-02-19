package com.cloudpulse.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a cloud infrastructure resource (EC2, RDS, ALB, Lambda, etc.)
 * being monitored by CloudPulse.
 */
@Entity
@Table(name = "resources", indexes = {
        @Index(name = "idx_resource_type", columnList = "resourceType"),
        @Index(name = "idx_resource_status", columnList = "status"),
        @Index(name = "idx_resource_region", columnList = "region")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Resource name is required")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Resource type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;

    @NotBlank(message = "Provider is required")
    @Column(nullable = false)
    private String provider; // aws, gcp, azure

    @NotBlank(message = "Region is required")
    @Column(nullable = false)
    private String region;

    @Column(unique = true)
    private String resourceId; // e.g., i-0abc123, arn:aws:rds:...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ResourceStatus status = ResourceStatus.UNKNOWN;

    private String ipAddress;

    private String environment; // prod, staging, dev

    @Column(columnDefinition = "TEXT")
    private String tags; // JSON string of key-value tags

    @Column(columnDefinition = "TEXT")
    private String metadata; // Additional JSON metadata

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastCheckedAt;

    public enum ResourceType {
        EC2, RDS, ALB, ELB, S3, LAMBDA, ECS, EKS, CLOUDFRONT, ELASTICACHE, DYNAMODB
    }

    public enum ResourceStatus {
        HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN, TERMINATED
    }
}
