package com.accessrisk.enums;

/**
 * Machine-readable error codes returned in every error response.
 *
 * Why this matters in production:
 *   Clients (UIs, integration teams) should never switch on human-readable message strings
 *   because those change. They switch on error codes. This decouples the display text
 *   from the handling logic and makes API contracts explicit.
 *
 * Convention: NOUN_VERB or DOMAIN_PROBLEM
 */
public enum ErrorCode {

    // ---- Resource lifecycle ----
    RESOURCE_NOT_FOUND,
    DUPLICATE_EMAIL,
    DUPLICATE_ROLE_NAME,
    DUPLICATE_PERMISSION_NAME,

    // ---- Assignment ----
    ROLE_ALREADY_ASSIGNED,
    PERMISSION_ALREADY_ASSIGNED,

    // ---- Business rule violations ----
    SELF_REFERENCING_RULE,          // permissionA == permissionB in a risk rule

    // ---- Data integrity (DB-level constraint violations) ----
    DATA_INTEGRITY_VIOLATION,

    // ---- Request structure ----
    VALIDATION_FAILED,              // @Valid body field errors
    INVALID_PARAMETER,              // path/query param type mismatch or constraint violation
    MALFORMED_REQUEST,              // unreadable JSON / missing body
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,
    ENDPOINT_NOT_FOUND,

    // ---- System ----
    INTERNAL_ERROR
}
