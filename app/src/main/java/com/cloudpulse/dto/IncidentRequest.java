package com.cloudpulse.dto;

import com.cloudpulse.model.Incident.IncidentStatus;
import com.cloudpulse.model.Incident.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for creating/updating an incident.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentRequest {

    @NotBlank(message = "Incident title is required")
    private String title;

    private String description;

    @NotNull(message = "Severity is required (CRITICAL, HIGH, MEDIUM, LOW, INFO)")
    private Severity severity;

    private Long resourceId;
    private String assignedTo;
    private IncidentStatus status;
    private String rootCause;
    private String resolution;
}
