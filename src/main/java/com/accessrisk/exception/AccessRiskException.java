package com.accessrisk.exception;

import com.accessrisk.enums.ErrorCode;
import lombok.Getter;

/**
 * Base class for all domain exceptions in this platform.
 *
 * Design decision: a common base type allows the GlobalExceptionHandler to
 * handle all domain errors in a single method (reading errorCode directly)
 * rather than duplicating the build logic in every catch block.
 * Individual exception types still exist for semantic clarity at throw sites.
 */
@Getter
public abstract class AccessRiskException extends RuntimeException {

    private final ErrorCode errorCode;

    protected AccessRiskException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
