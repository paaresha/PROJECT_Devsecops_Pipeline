package com.cloudpulse.config;

import com.cloudpulse.model.Incident;
import com.cloudpulse.model.Resource;
import com.cloudpulse.repository.IncidentRepository;
import com.cloudpulse.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the database with realistic infrastructure resources on startup.
 * Only runs in non-production profiles (dev, default).
 */
@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ResourceRepository resourceRepository;
    private final IncidentRepository incidentRepository;

    @Override
    public void run(String... args) {
        if (resourceRepository.count() > 0) {
            log.info("Database already seeded, skipping...");
            return;
        }

        log.info("🌱 Seeding database with demo infrastructure resources...");

        List<Resource> resources = List.of(
                Resource.builder()
                        .name("api-gateway-prod")
                        .resourceType(Resource.ResourceType.ALB)
                        .provider("aws")
                        .region("us-east-1")
                        .resourceId("arn:aws:elasticloadbalancing:us-east-1:123456789:loadbalancer/app/api-gw/abc123")
                        .status(Resource.ResourceStatus.HEALTHY)
                        .environment("prod")
                        .tags("{\"team\":\"platform\",\"cost-center\":\"engineering\"}")
                        .build(),

                Resource.builder()
                        .name("user-service-01")
                        .resourceType(Resource.ResourceType.EC2)
                        .provider("aws")
                        .region("us-east-1")
                        .resourceId("i-0a1b2c3d4e5f67890")
                        .ipAddress("10.0.1.42")
                        .status(Resource.ResourceStatus.HEALTHY)
                        .environment("prod")
                        .tags("{\"team\":\"backend\",\"service\":\"user-auth\"}")
                        .build(),

                Resource.builder()
                        .name("user-service-02")
                        .resourceType(Resource.ResourceType.EC2)
                        .provider("aws")
                        .region("us-east-1")
                        .resourceId("i-0b2c3d4e5f678901a")
                        .ipAddress("10.0.2.18")
                        .status(Resource.ResourceStatus.HEALTHY)
                        .environment("prod")
                        .tags("{\"team\":\"backend\",\"service\":\"user-auth\"}")
                        .build(),

                Resource.builder()
                        .name("orders-db-primary")
                        .resourceType(Resource.ResourceType.RDS)
                        .provider("aws")
                        .region("us-east-1")
                        .resourceId("arn:aws:rds:us-east-1:123456789:db:orders-primary")
                        .status(Resource.ResourceStatus.HEALTHY)
                        .environment("prod")
                        .metadata("{\"engine\":\"mysql\",\"version\":\"8.0\",\"multi-az\":true}")
                        .build(),

                Resource.builder()
                        .name("orders-db-replica")
                        .resourceType(Resource.ResourceType.RDS)
                        .provider("aws")
                        .region("us-west-2")
                        .resourceId("arn:aws:rds:us-west-2:123456789:db:orders-replica")
                        .status(Resource.ResourceStatus.HEALTHY)
                        .environment("prod")
                        .metadata("{\"engine\":\"mysql\",\"version\":\"8.0\",\"role\":\"read-replica\"}")
                        .build(),

                Resource.builder()
                        .name("session-cache")
                        .resourceType(Resource.ResourceType.ELASTICACHE)
                        .provider("aws")
                        .region("us-east-1")
                        .resourceId("arn:aws:elasticache:us-east-1:123456789:cluster:session-cache")
                        .status(Resource.ResourceStatus.HEALTHY)
                        .environment("prod")
                        .metadata("{\"engine\":\"redis\",\"version\":\"7.0\"}")
                        .build(),

                Resource.builder()
                        .name("payment-processor")
                        .resourceType(Resource.ResourceType.LAMBDA)
                        .provider("aws")
                        .region("us-east-1")
                        .resourceId("arn:aws:lambda:us-east-1:123456789:function:payment-processor")
                        .status(Resource.ResourceStatus.DEGRADED)
                        .environment("prod")
                        .build(),

                Resource.builder()
                        .name("static-assets-cdn")
                        .resourceType(Resource.ResourceType.CLOUDFRONT)
                        .provider("aws")
                        .region("global")
                        .resourceId("arn:aws:cloudfront::123456789:distribution/E1A2B3C4D5E6F7")
                        .status(Resource.ResourceStatus.HEALTHY)
                        .environment("prod")
                        .build(),

                Resource.builder()
                        .name("microservices-cluster")
                        .resourceType(Resource.ResourceType.EKS)
                        .provider("aws")
                        .region("us-east-1")
                        .resourceId("arn:aws:eks:us-east-1:123456789:cluster/microservices-prod")
                        .status(Resource.ResourceStatus.HEALTHY)
                        .environment("prod")
                        .metadata("{\"version\":\"1.29\",\"node-count\":6}")
                        .build(),

                Resource.builder()
                        .name("staging-web-server")
                        .resourceType(Resource.ResourceType.EC2)
                        .provider("aws")
                        .region("eu-west-1")
                        .resourceId("i-0c3d4e5f678901b2c")
                        .ipAddress("10.1.0.55")
                        .status(Resource.ResourceStatus.UNHEALTHY)
                        .environment("staging")
                        .build(),

                Resource.builder()
                        .name("analytics-data-store")
                        .resourceType(Resource.ResourceType.DYNAMODB)
                        .provider("aws")
                        .region("us-east-1")
                        .resourceId("arn:aws:dynamodb:us-east-1:123456789:table/analytics-events")
                        .status(Resource.ResourceStatus.HEALTHY)
                        .environment("prod")
                        .build(),

                Resource.builder()
                        .name("media-bucket")
                        .resourceType(Resource.ResourceType.S3)
                        .provider("aws")
                        .region("us-east-1")
                        .resourceId("arn:aws:s3:::media-assets-prod")
                        .status(Resource.ResourceStatus.HEALTHY)
                        .environment("prod")
                        .build());

        resourceRepository.saveAll(resources);
        log.info("✅ Seeded {} resources", resources.size());

        // Seed some incidents
        List<Incident> incidents = List.of(
                Incident.builder()
                        .title("Payment processor high latency")
                        .description(
                                "The payment-processor Lambda function is experiencing response times >5s, causing checkout failures.")
                        .severity(Incident.Severity.HIGH)
                        .status(Incident.IncidentStatus.INVESTIGATING)
                        .resource(resources.get(6)) // payment-processor
                        .assignedTo("oncall-backend")
                        .build(),

                Incident.builder()
                        .title("Staging web server unreachable")
                        .description(
                                "staging-web-server (eu-west-1) is not responding to health checks since 03:42 UTC.")
                        .severity(Incident.Severity.MEDIUM)
                        .status(Incident.IncidentStatus.OPEN)
                        .resource(resources.get(9)) // staging-web-server
                        .build(),

                Incident.builder()
                        .title("RDS replica replication lag >30s")
                        .description(
                                "The orders-db-replica in us-west-2 is showing replication lag exceeding 30 seconds.")
                        .severity(Incident.Severity.LOW)
                        .status(Incident.IncidentStatus.ACKNOWLEDGED)
                        .resource(resources.get(4)) // orders-db-replica
                        .assignedTo("dba-team")
                        .build());

        incidentRepository.saveAll(incidents);
        log.info("✅ Seeded {} incidents", incidents.size());
    }
}
