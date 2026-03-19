package com.accessrisk.exception;

import com.accessrisk.enums.ErrorCode;

/**
 * Thrown for violations of business rules not covered by resource-not-found or duplicate checks.
 * Maps to HTTP 400 via GlobalExceptionHandler.
 *
 * Examples: a risk rule referencing the same permission twice, invalid state transitions.
 */
public class BusinessException extends AccessRiskException {

    public BusinessException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
