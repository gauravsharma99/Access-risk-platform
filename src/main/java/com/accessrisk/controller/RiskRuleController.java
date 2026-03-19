package com.accessrisk.controller;

import com.accessrisk.dto.request.RiskRuleRequest;
import com.accessrisk.dto.response.RiskRuleResponse;
import com.accessrisk.service.RiskRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk-rules")
@RequiredArgsConstructor
@Tag(name = "Risk Rules", description = "Define Segregation of Duties (SoD) conflict rules")
public class RiskRuleController {

    private final RiskRuleService riskRuleService;

    @GetMapping
    @Operation(
        summary = "List all risk rules",
        description = "Returns all SoD rules including inactive ones. " +
                      "Only active rules are evaluated during risk analysis."
    )
    @ApiResponse(responseCode = "200", description = "Rules retrieved successfully")
    public ResponseEntity<List<RiskRuleResponse>> getAll() {
        return ResponseEntity.ok(riskRuleService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get risk rule by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule found"),
        @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<RiskRuleResponse> getById(
            @Parameter(description = "Risk rule ID") @PathVariable Long id) {
        return ResponseEntity.ok(riskRuleService.getById(id));
    }

    @PostMapping
    @Operation(
        summary = "Create a new risk rule",
        description = "Defines a conflicting permission pair. " +
                      "Example: CREATE_VENDOR + APPROVE_PAYMENT (financial fraud risk). " +
                      "permissionAId and permissionBId must be different."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rule created"),
        @ApiResponse(responseCode = "400", description = "Validation failed or same permission used twice"),
        @ApiResponse(responseCode = "404", description = "Permission A or B not found")
    })
    public ResponseEntity<RiskRuleResponse> create(@Valid @RequestBody RiskRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(riskRuleService.create(request));
    }
}
