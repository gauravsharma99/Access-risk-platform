package com.accessrisk.controller;

import com.accessrisk.dto.response.RiskViolationResponse;
import com.accessrisk.enums.ViolationStatus;
import com.accessrisk.service.RiskAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
@Tag(name = "Risk Analysis", description = "Run SoD analysis and inspect violations")
public class RiskAnalysisController {

    private final RiskAnalysisService riskAnalysisService;

    @PostMapping("/analyze")
    @Operation(
        summary = "Run risk analysis",
        description = """
                Executes SoD analysis against all active risk rules.

                - With no parameters: scans all users in the system.
                - With `userId`: scans only the specified user (useful after a role assignment).

                Only creates new violations — existing OPEN violations for the same
                user+rule combination are not duplicated.
                """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analysis complete — returns newly detected violations"),
        @ApiResponse(responseCode = "404", description = "User not found (when userId is provided)")
    })
    public ResponseEntity<List<RiskViolationResponse>> analyze(
            @Parameter(description = "Optional: restrict analysis to a single user")
            @RequestParam(required = false) Long userId) {

        List<RiskViolationResponse> violations = (userId != null)
                ? riskAnalysisService.analyzeUser(userId)
                : riskAnalysisService.analyzeAll();

        return ResponseEntity.ok(violations);
    }

    @GetMapping("/violations")
    @Operation(
        summary = "Get all violations",
        description = "Returns all recorded violations. Optionally filter by status."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Violations retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    public ResponseEntity<List<RiskViolationResponse>> getViolations(
            @Parameter(description = "Filter by violation status (OPEN, MITIGATED, ACCEPTED, RESOLVED)")
            @RequestParam(required = false) ViolationStatus status) {

        List<RiskViolationResponse> violations = (status != null)
                ? riskAnalysisService.getViolationsByStatus(status)
                : riskAnalysisService.getAllViolations();

        return ResponseEntity.ok(violations);
    }
}
