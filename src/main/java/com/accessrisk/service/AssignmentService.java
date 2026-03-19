package com.accessrisk.service;

import com.accessrisk.entity.Permission;
import com.accessrisk.entity.Role;
import com.accessrisk.entity.RolePermission;
import com.accessrisk.entity.User;
import com.accessrisk.entity.UserRole;
import com.accessrisk.enums.AuditAction;
import com.accessrisk.enums.ErrorCode;
import com.accessrisk.exception.DuplicateResourceException;
import com.accessrisk.repository.RolePermissionRepository;
import com.accessrisk.repository.UserRoleRepository;
import com.accessrisk.util.AuditDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;

    /**
     * Assigns a role to a user.
     *
     * Validates that both entities exist before creating the assignment.
     * Throws 409 if the same role is already assigned — surfaces accidental double-calls.
     */
    @Transactional
    public void assignRoleToUser(Long userId, Long roleId) {
        User user = userService.findOrThrow(userId);
        Role role = roleService.findOrThrow(roleId);

        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new DuplicateResourceException(
                    "Role '" + role.getName() + "' is already assigned to user '" + user.getName() + "'",
                    ErrorCode.ROLE_ALREADY_ASSIGNED
            );
        }

        UserRole assignment = UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .build();

        userRoleRepository.save(assignment);

        auditLogService.log(
                AuditAction.ROLE_ASSIGNED_TO_USER,
                "UserRole",
                assignment.getId(),
                AuditDetails.of(
                        "userId", userId,
                        "userName", user.getName(),
                        "roleId", roleId,
                        "roleName", role.getName()
                )
        );

        log.info("Role assigned: roleId={} roleName={} → userId={} userName={}",
                roleId, role.getName(), userId, user.getName());
    }

    /**
     * Assigns a permission to a role.
     *
     * Validates that both entities exist before creating the assignment.
     * Throws 409 if the permission is already on the role.
     */
    @Transactional
    public void assignPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleService.findOrThrow(roleId);
        Permission permission = permissionService.findOrThrow(permissionId);

        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw new DuplicateResourceException(
                    "Permission '" + permission.getName() + "' is already assigned to role '" + role.getName() + "'",
                    ErrorCode.PERMISSION_ALREADY_ASSIGNED
            );
        }

        RolePermission assignment = RolePermission.builder()
                .roleId(roleId)
                .permissionId(permissionId)
                .build();

        rolePermissionRepository.save(assignment);

        auditLogService.log(
                AuditAction.PERMISSION_ASSIGNED_TO_ROLE,
                "RolePermission",
                assignment.getId(),
                AuditDetails.of(
                        "roleId", roleId,
                        "roleName", role.getName(),
                        "permissionId", permissionId,
                        "permissionName", permission.getName()
                )
        );

        log.info("Permission assigned: permissionId={} permissionName={} → roleId={} roleName={}",
                permissionId, permission.getName(), roleId, role.getName());
    }
}
