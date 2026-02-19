package com.cloudpulse.service;

import com.cloudpulse.dto.ResourceRequest;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.model.Resource;
import com.cloudpulse.model.Resource.ResourceStatus;
import com.cloudpulse.model.Resource.ResourceType;
import com.cloudpulse.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceService Unit Tests")
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    private Resource testResource;
    private ResourceRequest testRequest;

    @BeforeEach
    void setUp() {
        testResource = Resource.builder()
                .id(1L)
                .name("test-server")
                .resourceType(ResourceType.EC2)
                .provider("aws")
                .region("us-east-1")
                .resourceId("i-0abc123def456")
                .status(ResourceStatus.HEALTHY)
                .environment("prod")
                .build();

        testRequest = ResourceRequest.builder()
                .name("test-server")
                .resourceType(ResourceType.EC2)
                .provider("aws")
                .region("us-east-1")
                .resourceId("i-0abc123def456")
                .environment("prod")
                .build();
    }

    @Test
    @DisplayName("Should return all resources")
    void getAllResources_ReturnsAllResources() {
        when(resourceRepository.findAll()).thenReturn(List.of(testResource));

        List<Resource> result = resourceService.getAllResources();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("test-server");
        verify(resourceRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return resource by ID")
    void getResourceById_ExistingId_ReturnsResource() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));

        Resource result = resourceService.getResourceById(1L);

        assertThat(result.getName()).isEqualTo("test-server");
        assertThat(result.getResourceType()).isEqualTo(ResourceType.EC2);
    }

    @Test
    @DisplayName("Should throw exception for non-existent resource")
    void getResourceById_NonExistentId_ThrowsException() {
        when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.getResourceById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Should create a new resource")
    void createResource_ValidRequest_ReturnsCreatedResource() {
        when(resourceRepository.save(any(Resource.class))).thenReturn(testResource);

        Resource result = resourceService.createResource(testRequest);

        assertThat(result.getName()).isEqualTo("test-server");
        assertThat(result.getProvider()).isEqualTo("aws");
        verify(resourceRepository, times(1)).save(any(Resource.class));
    }

    @Test
    @DisplayName("Should update resource status")
    void updateStatus_ValidStatus_UpdatesAndReturns() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(testResource);

        Resource result = resourceService.updateStatus(1L, ResourceStatus.DEGRADED);

        assertThat(result).isNotNull();
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    @DisplayName("Should delete existing resource")
    void deleteResource_ExistingId_DeletesSuccessfully() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        doNothing().when(resourceRepository).delete(testResource);

        assertThatCode(() -> resourceService.deleteResource(1L))
                .doesNotThrowAnyException();
        verify(resourceRepository, times(1)).delete(testResource);
    }

    @Test
    @DisplayName("Should filter resources by type")
    void getByType_EC2_ReturnsFilteredList() {
        when(resourceRepository.findByResourceType(ResourceType.EC2))
                .thenReturn(List.of(testResource));

        List<Resource> result = resourceService.getByType(ResourceType.EC2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getResourceType()).isEqualTo(ResourceType.EC2);
    }

    @Test
    @DisplayName("Should return unhealthy resources")
    void getUnhealthy_ReturnsUnhealthyAndDegraded() {
        Resource degraded = Resource.builder()
                .id(2L).name("slow-server").status(ResourceStatus.DEGRADED).build();

        when(resourceRepository.findByStatusIn(
                List.of(ResourceStatus.UNHEALTHY, ResourceStatus.DEGRADED)))
                .thenReturn(List.of(degraded));

        List<Resource> result = resourceService.getUnhealthy();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ResourceStatus.DEGRADED);
    }
}
