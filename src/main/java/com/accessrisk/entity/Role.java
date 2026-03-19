package com.accessrisk.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A named grouping of permissions (e.g. "Accounts Payable Clerk", "Finance Manager").
 * Users are assigned roles; roles carry permissions.
 */
@Entity
@Table(
    name = "roles",
    uniqueConstraints = @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;
}
