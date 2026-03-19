package com.accessrisk.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Assignment of a role to a user.
 *
 * Design note: We model this as an explicit entity (not @ManyToMany) for two reasons:
 * 1. It gives us a primary key we can reference in audit logs.
 * 2. It makes it easy to add future fields like assignedBy, validFrom, validTo (role mining).
 *
 * The composite unique constraint prevents duplicate assignments.
 */
@Entity
@Table(
    name = "user_roles",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_roles_user_role",
        columnNames = {"user_id", "role_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;
}
