package com.accessrisk.enums;

/**
 * Exhaustive list of auditable operations in the platform.
 *
 * Why an enum instead of String constants:
 *   - Compile-time safety: you cannot accidentally log "USER_CREATD"
 *   - Discoverability: the full audit surface is visible in one place
 *   - Queryability: audit logs can be filtered/grouped by action without text matching
 *   - Documentation: every event is self-describing by name
 *
 * Naming convention: ENTITY_PAST_TENSE_VERB
 */
public enum AuditAction {

    // ---- User lifecycle ----
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,

    // ---- Role lifecycle ----
    ROLE_CREATED,

    // ---- Permission lifecycle ----
    PERMISSION_CREATED,

    // ---- Assignments ----
    ROLE_ASSIGNED_TO_USER,
    PERMISSION_ASSIGNED_TO_ROLE,

    // ---- Risk configuration ----
    RISK_RULE_CREATED,

    // ---- Risk analysis ----
    RISK_ANALYSIS_STARTED,
    RISK_ANALYSIS_COMPLETED,
    VIOLATION_DETECTED
}
