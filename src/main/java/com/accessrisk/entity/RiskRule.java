package com.accessrisk.entity;

import com.accessrisk.enums.Severity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Defines a Segregation of Duties (SoD) conflict between two permissions.
 *
 * Example: permissionA = CREATE_VENDOR, permissionB = APPROVE_PAYMENT
 * If a user holds both, a RiskViolation is raised.
 *
 * Rules are stored by permission ID (not name) to remain stable if permissions are renamed.
 * The "active" flag allows rules to be disabled without deletion — important for audit history.
 */
@Entity
@Table(name = "risk_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRule extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "permission_a_id", nullable = false)
    private Long permissionAId;

    @Column(name = "permission_b_id", nullable = false)
    private Long permissionBId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
