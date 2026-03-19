package com.accessrisk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Common fields shared across all auditable entities.
 *
 * Design note: We use @MappedSuperclass (not @Entity) so this class is never
 * mapped to its own table. Each subclass gets its own table with these columns
 * inlined — this is the TABLE_PER_CLASS strategy without the inheritance overhead.
 *
 * ID strategy: IDENTITY delegates to PostgreSQL's native identity column (BIGSERIAL).
 * This is simpler and more reliable than SEQUENCE with per-subclass @SequenceGenerator
 * annotations, which require careful generator name scoping in Hibernate 6.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
