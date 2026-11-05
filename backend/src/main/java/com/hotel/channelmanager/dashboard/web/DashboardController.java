package com.hotel.channelmanager.dashboard.web;

import com.hotel.channelmanager.dashboard.dto.DashboardSummaryResponse;
import com.hotel.channelmanager.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/dashboard")
@Tag(name = "Dashboard", description = "Real-time hotel KPIs, occupancy rate, arrivals, departures, revenue, and channel status")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get complete dashboard summary for property")
    public ResponseEntity<DashboardSummaryResponse> getDashboard(@PathVariable String propertyId) {
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(propertyId);
        return ResponseEntity.ok(response);
    }
}

