package com.cloudpulse.dto;

import com.cloudpulse.model.Resource.ResourceStatus;
import com.cloudpulse.model.Resource.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for creating/updating a monitored resource.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceRequest {

    @NotBlank(message = "Resource name is required")
    private String name;

    @NotNull(message = "Resource type is required")
    private ResourceType resourceType;

    @NotBlank(message = "Provider is required (aws, gcp, azure)")
    private String provider;

    @NotBlank(message = "Region is required")
    private String region;

    private String resourceId;
    private String ipAddress;
    private String environment;
    private String tags;
    private String metadata;
    private ResourceStatus status;
}
