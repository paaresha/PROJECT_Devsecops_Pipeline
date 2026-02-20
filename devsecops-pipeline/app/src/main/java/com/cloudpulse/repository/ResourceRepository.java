package com.cloudpulse.repository;

import com.cloudpulse.model.Resource;
import com.cloudpulse.model.Resource.ResourceStatus;
import com.cloudpulse.model.Resource.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByResourceId(String resourceId);

    List<Resource> findByResourceType(ResourceType resourceType);

    List<Resource> findByStatus(ResourceStatus status);

    List<Resource> findByRegion(String region);

    List<Resource> findByProvider(String provider);

    List<Resource> findByEnvironment(String environment);

    @Query("SELECT r FROM Resource r WHERE r.status IN :statuses")
    List<Resource> findByStatusIn(@Param("statuses") List<ResourceStatus> statuses);

    @Query("SELECT r.status, COUNT(r) FROM Resource r GROUP BY r.status")
    List<Object[]> countByStatus();

    @Query("SELECT r.resourceType, COUNT(r) FROM Resource r GROUP BY r.resourceType")
    List<Object[]> countByResourceType();

    @Query("SELECT r.region, COUNT(r) FROM Resource r GROUP BY r.region")
    List<Object[]> countByRegion();

    @Query("SELECT COUNT(r) FROM Resource r WHERE r.status = 'UNHEALTHY' OR r.status = 'DEGRADED'")
    long countUnhealthyResources();
}
