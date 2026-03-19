package com.accessrisk.exception;

import com.accessrisk.enums.ErrorCode;

/**
 * Thrown when a requested entity does not exist in the database.
 * Maps to HTTP 404 via GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends AccessRiskException {

    public ResourceNotFoundException(String resourceType, Long id) {
        super(resourceType + " not found with id: " + id, ErrorCode.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceType, String field, String value) {
        super(resourceType + " not found with " + field + ": " + value, ErrorCode.RESOURCE_NOT_FOUND);
    }
}
