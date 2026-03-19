package com.accessrisk.controller;

import com.accessrisk.dto.response.MessageResponse;
import com.accessrisk.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles role-to-user and permission-to-role assignments.
 *
 * URL design note: assignment endpoints are modelled as sub-resources of the
 * "parent" entity they modify (/api/users/{id}/roles, /api/roles/{id}/permissions).
 * This follows REST conventions for many-to-many relationship management and keeps
 * intent clear without introducing a dedicated /assignments resource.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Assignments", description = "Assign roles to users and permissions to roles")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/api/users/{userId}/roles/{roleId}")
    @Operation(
        summary = "Assign a role to a user",
        description = "Grants the user all permissions bundled in the specified role. " +
                      "Re-assigning the same role returns 409."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
        @ApiResponse(responseCode = "404", description = "User or Role not found"),
        @ApiResponse(responseCode = "409", description = "Role already assigned to this user")
    })
    public ResponseEntity<MessageResponse> assignRoleToUser(
            @Parameter(description = "ID of the user") @PathVariable Long userId,
            @Parameter(description = "ID of the role to assign") @PathVariable Long roleId) {

        assignmentService.assignRoleToUser(userId, roleId);
        return ResponseEntity.ok(MessageResponse.of(
                String.format("Role %d successfully assigned to user %d", roleId, userId)
        ));
    }

    @PostMapping("/api/roles/{roleId}/permissions/{permissionId}")
    @Operation(
        summary = "Assign a permission to a role",
        description = "All users holding this role will gain the specified permission. " +
                      "Re-assigning the same permission returns 409."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permission assigned successfully"),
        @ApiResponse(responseCode = "404", description = "Role or Permission not found"),
        @ApiResponse(responseCode = "409", description = "Permission already assigned to this role")
    })
    public ResponseEntity<MessageResponse> assignPermissionToRole(
            @Parameter(description = "ID of the role") @PathVariable Long roleId,
            @Parameter(description = "ID of the permission to assign") @PathVariable Long permissionId) {

        assignmentService.assignPermissionToRole(roleId, permissionId);
        return ResponseEntity.ok(MessageResponse.of(
                String.format("Permission %d successfully assigned to role %d", permissionId, roleId)
        ));
    }
}
