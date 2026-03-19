package com.accessrisk.exception;

import com.accessrisk.enums.ErrorCode;

/**
 * Thrown when a create/assign operation would violate a uniqueness constraint.
 * Maps to HTTP 409 via GlobalExceptionHandler.
 */
public class DuplicateResourceException extends AccessRiskException {

    public DuplicateResourceException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }

    /** Convenience constructor when a specific ErrorCode is not needed. */
    public DuplicateResourceException(String message) {
        super(message, ErrorCode.DATA_INTEGRITY_VIOLATION);
    }
}
