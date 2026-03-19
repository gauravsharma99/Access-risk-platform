package com.accessrisk.enums;

/**
 * Lifecycle states for a detected risk violation.
 *
 * OPEN       - Newly detected, requires review.
 * MITIGATED  - Compensating controls are in place (violation acknowledged but managed).
 * ACCEPTED   - Risk accepted by a risk owner with documented justification.
 * RESOLVED   - Underlying access has been remediated; violation is closed.
 */
public enum ViolationStatus {
    OPEN,
    MITIGATED,
    ACCEPTED,
    RESOLVED
}
