package com.accessrisk.controller;

import com.accessrisk.dto.request.PermissionRequest;
import com.accessrisk.dto.response.PermissionResponse;
import com.accessrisk.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "Manage atomic business capabilities (e.g. CREATE_VENDOR)")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(summary = "List all permissions")
    @ApiResponse(responseCode = "200", description = "Permissions retrieved successfully")
    public ResponseEntity<List<PermissionResponse>> getAll() {
        return ResponseEntity.ok(permissionService.getAll());
    }

    @PostMapping
    @Operation(
        summary = "Create a new permission",
        description = "Permission names must be UPPER_SNAKE_CASE (e.g. CREATE_VENDOR, APPROVE_PAYMENT)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Permission created"),
        @ApiResponse(responseCode = "400", description = "Validation failed or invalid name format"),
        @ApiResponse(responseCode = "409", description = "Permission name already exists")
    })
    public ResponseEntity<PermissionResponse> create(@Valid @RequestBody PermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.create(request));
    }
}
