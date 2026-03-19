package com.accessrisk.entity;

import com.accessrisk.enums.Severity;
import com.accessrisk.enums.ViolationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A detected instance of a user violating a RiskRule.
 *
 * Design note: "detectedAt" is separate from BaseEntity.createdAt to make it explicit
 * that this timestamp reflects when the analysis job ran, not record creation time.
 * They will often be the same, but keeping them distinct preserves semantic clarity.
 *
 * The "explanation" field stores a human-readable description generated at detection time,
 * capturing the state of permissions at that moment — important for historical accuracy
 * if permissions change after detection.
 */
@Entity
@Table(name = "risk_violations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskViolation extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ViolationStatus status = ViolationStatus.OPEN;

    @Column(length = 2000)
    private String explanation;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;
}
