package com.accessrisk.dto.response;

import com.accessrisk.enums.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform error envelope returned for all 4xx/5xx responses.
 *
 * Fields:
 *   status     - HTTP status code (integer, for convenience alongside the HTTP status line)
 *   error      - HTTP status phrase (e.g. "Not Found")
 *   errorCode  - Machine-readable enum — clients switch on this, not on 'message'
 *   message    - Human-readable description safe to display in UI or logs
 *   path       - Request URI, useful for correlating errors in client-side logs
 *   traceId    - Unique ID per error response; include in support tickets to correlate server logs
 *   timestamp  - When the error occurred (server time)
 *   fieldErrors - Only present on 400 validation errors; maps field names to constraint messages
 *
 * @JsonInclude(NON_NULL) ensures fieldErrors and errorCode are omitted from the JSON
 * when not applicable (e.g. 500 errors don't have an errorCode or fieldErrors).
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private ErrorCode errorCode;
    private String message;
    private String path;
    private String traceId;
    private LocalDateTime timestamp;

    /**
     * Per-field validation failures — only present on 400 validation errors.
     * Key: field name (matches the JSON property in the request body).
     * Value: constraint violation message.
     */
    private Map<String, String> fieldErrors;
}
