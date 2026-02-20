package com.cloudpulse.controller;

import com.cloudpulse.dto.DashboardSummary;
import com.cloudpulse.service.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@DisplayName("DashboardController Integration Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    @DisplayName("GET /api/dashboard — returns aggregated health summary")
    void getDashboard_ReturnsHealthSummary() throws Exception {
        DashboardSummary summary = DashboardSummary.builder()
                .totalResources(12)
                .healthyResources(9)
                .unhealthyResources(1)
                .degradedResources(1)
                .overallHealthPercent(75.0)
                .activeIncidents(3)
                .criticalIncidents(0)
                .mttrMinutes(45.5)
                .healthChecksLast24h(288)
                .resourcesByType(Map.of("EC2", 3L, "RDS", 2L))
                .resourcesByStatus(Map.of("HEALTHY", 9L, "DEGRADED", 1L))
                .build();

        when(dashboardService.getDashboardSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResources").value(12))
                .andExpect(jsonPath("$.healthyResources").value(9))
                .andExpect(jsonPath("$.overallHealthPercent").value(75.0))
                .andExpect(jsonPath("$.activeIncidents").value(3))
                .andExpect(jsonPath("$.resourcesByType.EC2").value(3));
    }
}
