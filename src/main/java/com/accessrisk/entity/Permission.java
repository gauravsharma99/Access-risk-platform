package com.accessrisk.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * An atomic capability in the system (e.g. "CREATE_VENDOR", "APPROVE_PAYMENT").
 *
 * Permissions are intentionally coarse-grained at the business action level,
 * not at the HTTP endpoint level. This mirrors SAP SoD (Segregation of Duties) design.
 */
@Entity
@Table(
    name = "permissions",
    uniqueConstraints = @UniqueConstraint(name = "uk_permissions_name", columnNames = "name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;
}
