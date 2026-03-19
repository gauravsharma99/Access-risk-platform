package com.accessrisk.controller;

import com.accessrisk.dto.response.AuditLogResponse;
import com.accessrisk.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Audit log endpoints are intentionally read-only.
 * Logs are written exclusively by the AuditLogService — never via this API.
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Immutable record of all platform operations (read-only)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(
        summary = "Get paginated audit log",
        description = """
                Returns audit events ordered by most recent first.

                Supports standard Spring pagination parameters:
                - `page` (0-based, default 0)
                - `size` (default 20, max recommended 100)
                - `sort` (e.g. `timestamp,desc`)
                """
    )
    @ApiResponse(responseCode = "200", description = "Audit log retrieved successfully")
    public ResponseEntity<Page<AuditLogResponse>> getAll(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAll(pageable));
    }

    @GetMapping("/{entityType}/{entityId}")
    @Operation(
        summary = "Get audit history for a specific entity",
        description = "Returns all audit events for a given entity. " +
                      "entityType examples: User, Role, Permission, RiskViolation"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audit history retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid entityType or entityId")
    })
    public ResponseEntity<List<AuditLogResponse>> getByEntity(
            @Parameter(description = "Entity type (e.g. User, Role, RiskViolation)")
            @PathVariable String entityType,
            @Parameter(description = "Entity ID")
            @PathVariable Long entityId) {
        return ResponseEntity.ok(auditLogService.getByEntity(entityType, entityId));
    }
}
