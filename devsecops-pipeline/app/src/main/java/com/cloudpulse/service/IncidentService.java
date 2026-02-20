package com.cloudpulse.service;

import com.cloudpulse.dto.IncidentRequest;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.model.Incident;
import com.cloudpulse.model.Incident.IncidentStatus;
import com.cloudpulse.model.Resource;
import com.cloudpulse.repository.IncidentRepository;
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
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final ResourceRepository resourceRepository;

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    public List<Incident> getActiveIncidents() {
        return incidentRepository.findActiveIncidents();
    }

    public List<Incident> getActiveCriticalIncidents() {
        return incidentRepository.findActiveCriticalIncidents();
    }

    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    public List<Incident> getIncidentsByResource(Long resourceId) {
        return incidentRepository.findByResourceId(resourceId);
    }

    @Transactional
    public Incident createIncident(IncidentRequest request) {
        Incident.IncidentBuilder builder = Incident.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .assignedTo(request.getAssignedTo());

        // Link to a resource if specified
        if (request.getResourceId() != null) {
            Resource resource = resourceRepository.findById(request.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resource", request.getResourceId()));
            builder.resource(resource);
        }

        Incident incident = builder.build();
        Incident saved = incidentRepository.save(incident);
        log.warn("🚨 Incident created: [{}] {} (Severity: {})",
                saved.getId(), saved.getTitle(), saved.getSeverity());
        return saved;
    }

    @Transactional
    public Incident acknowledgeIncident(Long id) {
        Incident incident = getIncidentById(id);
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedAt(LocalDateTime.now());
        log.info("Incident {} acknowledged", id);
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident resolveIncident(Long id, String rootCause, String resolution) {
        Incident incident = getIncidentById(id);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setRootCause(rootCause);
        incident.setResolution(resolution);
        incident.setResolvedAt(LocalDateTime.now());
        log.info("Incident {} resolved: {}", id, resolution);
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident updateIncident(Long id, IncidentRequest request) {
        Incident incident = getIncidentById(id);
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setSeverity(request.getSeverity());
        incident.setAssignedTo(request.getAssignedTo());
        if (request.getStatus() != null) {
            incident.setStatus(request.getStatus());
        }
        if (request.getRootCause() != null) {
            incident.setRootCause(request.getRootCause());
        }
        if (request.getResolution() != null) {
            incident.setResolution(request.getResolution());
        }
        return incidentRepository.save(incident);
    }

    @Transactional
    public void deleteIncident(Long id) {
        Incident incident = getIncidentById(id);
        incidentRepository.delete(incident);
        log.info("Incident {} deleted", id);
    }

    public long getActiveCount() {
        return incidentRepository.findActiveIncidents().size();
    }

    public long getCriticalCount() {
        return incidentRepository.findActiveCriticalIncidents().size();
    }
}
