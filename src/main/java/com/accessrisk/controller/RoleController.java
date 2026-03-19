package com.accessrisk.controller;

import com.accessrisk.dto.request.RoleRequest;
import com.accessrisk.dto.response.RoleResponse;
import com.accessrisk.service.RoleService;
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
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Manage roles that bundle permissions")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "List all roles")
    @ApiResponse(responseCode = "200", description = "Roles retrieved successfully")
    public ResponseEntity<List<RoleResponse>> getAll() {
        return ResponseEntity.ok(roleService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role found"),
        @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<RoleResponse> getById(
            @Parameter(description = "Role ID") @PathVariable Long id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new role")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Role created"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "409", description = "Role name already exists")
    })
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
    }
}
