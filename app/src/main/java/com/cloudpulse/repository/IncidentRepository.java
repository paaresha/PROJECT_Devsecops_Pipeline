package com.cloudpulse.repository;

import com.cloudpulse.model.Incident;
import com.cloudpulse.model.Incident.IncidentStatus;
import com.cloudpulse.model.Incident.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findBySeverity(Severity severity);

    List<Incident> findByResourceId(Long resourceId);

    @Query("SELECT i FROM Incident i WHERE i.status NOT IN ('RESOLVED', 'CLOSED') ORDER BY i.severity ASC, i.createdAt ASC")
    List<Incident> findActiveIncidents();

    @Query("SELECT i FROM Incident i WHERE i.severity = 'CRITICAL' AND i.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Incident> findActiveCriticalIncidents();

    @Query("SELECT i.severity, COUNT(i) FROM Incident i WHERE i.status NOT IN ('RESOLVED', 'CLOSED') GROUP BY i.severity")
    List<Object[]> countActiveIncidentsBySeverity();

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.createdAt >= :since")
    long countIncidentsSince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, i.createdAt, i.resolvedAt)) FROM Incident i WHERE i.resolvedAt IS NOT NULL AND i.createdAt >= :since")
    Double avgResolutionTimeMinutesSince(@Param("since") LocalDateTime since);
}
