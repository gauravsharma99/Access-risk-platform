package com.accessrisk.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Assignment of a permission to a role.
 *
 * Same design rationale as UserRole — explicit entity over @ManyToMany.
 * A unique constraint ensures a permission cannot be added to the same role twice.
 */
@Entity
@Table(
    name = "role_permissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_role_permissions_role_perm",
        columnNames = {"role_id", "permission_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission extends BaseEntity {

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;
}
