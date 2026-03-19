package com.accessrisk.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable record of every significant state-changing operation in the system.
 *
 * Design notes:
 * - This entity intentionally does NOT extend BaseEntity. AuditLog has its own
 *   timestamp semantics and should never be modified after creation.
 * - No @Setter on the class — all fields set via constructor/builder only.
 * - "entityId" is nullable: some actions (e.g. bulk analysis) don't target a single entity.
 * - "details" holds a JSON snapshot or free-text description of the change.
 * - "createdBy" is a string (username/email) rather than a FK, so logs survive user deletion.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requires no-arg; keep it restricted
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;           // e.g. USER_CREATED, ROLE_ASSIGNED, VIOLATION_DETECTED

    @Column(name = "entity_type", nullable = false)
    private String entityType;       // e.g. "User", "Role", "RiskViolation"

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 4000)
    private String details;          // JSON or human-readable change summary

    @Column(name = "created_by", nullable = false)
    private String createdBy;        // username or "SYSTEM" for automated operations

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
