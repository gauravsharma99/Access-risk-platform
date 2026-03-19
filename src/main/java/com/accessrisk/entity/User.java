package com.accessrisk.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a system user who can be assigned roles.
 *
 * Table name is explicitly "users" because "user" is a reserved keyword in PostgreSQL.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String department;
}
