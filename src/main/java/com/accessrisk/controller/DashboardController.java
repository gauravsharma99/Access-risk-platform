package com.accessrisk.controller;

import com.accessrisk.dto.response.DashboardSummaryResponse;
import com.accessrisk.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Platform-wide risk and access governance metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(
        summary = "Get platform summary",
        description = """
                Returns aggregated metrics for the governance dashboard:
                - Total counts for users, roles, permissions, and active rules.
                - Open violation count (requires immediate attention).
                - Violation breakdown by severity (CRITICAL, HIGH, MEDIUM, LOW).
                """
    )
    @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
