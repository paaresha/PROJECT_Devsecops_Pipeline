package com.cloudpulse.repository;

import com.cloudpulse.model.HealthCheck;
import com.cloudpulse.model.HealthCheck.HealthStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long> {

    List<HealthCheck> findByResourceIdOrderByCheckedAtDesc(Long resourceId, Pageable pageable);

    @Query("SELECT h FROM HealthCheck h WHERE h.resource.id = :resourceId ORDER BY h.checkedAt DESC")
    List<HealthCheck> findLatestByResourceId(@Param("resourceId") Long resourceId, Pageable pageable);

    List<HealthCheck> findByStatus(HealthStatus status);

    @Query("SELECT h FROM HealthCheck h WHERE h.checkedAt >= :since ORDER BY h.checkedAt DESC")
    List<HealthCheck> findRecentChecks(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(h.responseTimeMs) FROM HealthCheck h WHERE h.resource.id = :resourceId AND h.checkedAt >= :since")
    Double avgResponseTimeByResourceSince(@Param("resourceId") Long resourceId, @Param("since") LocalDateTime since);

    @Query("SELECT h.status, COUNT(h) FROM HealthCheck h WHERE h.checkedAt >= :since GROUP BY h.status")
    List<Object[]> countByStatusSince(@Param("since") LocalDateTime since);
}
