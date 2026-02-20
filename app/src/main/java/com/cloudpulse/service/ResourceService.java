package com.cloudpulse.service;

import com.cloudpulse.dto.ResourceRequest;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.model.Resource;
import com.cloudpulse.model.Resource.ResourceStatus;
import com.cloudpulse.model.Resource.ResourceType;
import com.cloudpulse.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
    }

    public Resource getByResourceId(String resourceId) {
        return resourceRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));
    }

    public List<Resource> getByType(ResourceType type) {
        return resourceRepository.findByResourceType(type);
    }

    public List<Resource> getByStatus(ResourceStatus status) {
        return resourceRepository.findByStatus(status);
    }

    public List<Resource> getByRegion(String region) {
        return resourceRepository.findByRegion(region);
    }

    public List<Resource> getByProvider(String provider) {
        return resourceRepository.findByProvider(provider);
    }

    public List<Resource> getUnhealthy() {
        return resourceRepository.findByStatusIn(
                List.of(ResourceStatus.UNHEALTHY, ResourceStatus.DEGRADED));
    }

    @Transactional
    public Resource createResource(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .resourceType(request.getResourceType())
                .provider(request.getProvider())
                .region(request.getRegion())
                .resourceId(request.getResourceId())
                .ipAddress(request.getIpAddress())
                .environment(request.getEnvironment())
                .tags(request.getTags())
                .metadata(request.getMetadata())
                .status(request.getStatus() != null ? request.getStatus() : ResourceStatus.UNKNOWN)
                .build();

        Resource saved = resourceRepository.save(resource);
        log.info("Created resource: {} ({})", saved.getName(), saved.getResourceType());
        return saved;
    }

    @Transactional
    public Resource updateResource(Long id, ResourceRequest request) {
        Resource resource = getResourceById(id);

        resource.setName(request.getName());
        resource.setResourceType(request.getResourceType());
        resource.setProvider(request.getProvider());
        resource.setRegion(request.getRegion());
        resource.setResourceId(request.getResourceId());
        resource.setIpAddress(request.getIpAddress());
        resource.setEnvironment(request.getEnvironment());
        resource.setTags(request.getTags());
        resource.setMetadata(request.getMetadata());
        if (request.getStatus() != null) {
            resource.setStatus(request.getStatus());
        }

        Resource saved = resourceRepository.save(resource);
        log.info("Updated resource: {} (ID: {})", saved.getName(), saved.getId());
        return saved;
    }

    @Transactional
    public Resource updateStatus(Long id, ResourceStatus status) {
        Resource resource = getResourceById(id);
        resource.setStatus(status);
        resource.setLastCheckedAt(LocalDateTime.now());
        return resourceRepository.save(resource);
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = getResourceById(id);
        resourceRepository.delete(resource);
        log.info("Deleted resource: {} (ID: {})", resource.getName(), id);
    }

    public long getTotalCount() {
        return resourceRepository.count();
    }

    public long getUnhealthyCount() {
        return resourceRepository.countUnhealthyResources();
    }
}
