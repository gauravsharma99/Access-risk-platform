package com.accessrisk.exception;

import com.accessrisk.dto.response.ErrorResponse;
import com.accessrisk.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Single entry point for all exception-to-HTTP-response translation.
 *
 * Ordering within this class matters: Spring picks the most specific matching
 * @ExceptionHandler. More specific exceptions are listed before broader ones.
 *
 * Every error response includes a traceId — a unique identifier generated per
 * error occurrence. Clients include this in support requests; ops teams use it
 * to grep the exact log entry. In a distributed system, replace UUID.randomUUID()
 * with MDC.get("traceId") once OpenTelemetry or Sleuth is wired in.
 *
 * Principle: log details server-side, expose only safe summaries to clients.
 * Never expose stack traces, internal class names, or SQL snippets externally.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // Domain exceptions — our own hierarchy
    // -------------------------------------------------------------------------

    /**
     * Handles all subclasses of AccessRiskException in one method.
     * The specific HTTP status is determined by the concrete type.
     */
    @ExceptionHandler(AccessRiskException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(
            AccessRiskException ex, HttpServletRequest request) {

        HttpStatus status = resolveStatus(ex);
        String traceId = traceId();

        log.warn("[{}] Domain exception on {} {}: {} (errorCode={})",
                traceId, request.getMethod(), request.getRequestURI(),
                ex.getMessage(), ex.getErrorCode());

        return build(status, ex.getErrorCode(), ex.getMessage(), request, traceId);
    }

    // -------------------------------------------------------------------------
    // Validation exceptions
    // -------------------------------------------------------------------------

    /**
     * Triggered by @Valid on @RequestBody parameters.
     * Returns a fieldErrors map so the client knows exactly which fields to correct.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                // putIfAbsent preserves the first violation per field (Spring already returns one per field,
                // but this guard is defensive against future framework changes)
                fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage())
        );

        String traceId = traceId();
        log.debug("[{}] Validation failed on {} {}: {} field error(s)",
                traceId, request.getMethod(), request.getRequestURI(), fieldErrors.size());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .errorCode(ErrorCode.VALIDATION_FAILED)
                .message("Request validation failed — see fieldErrors for details")
                .path(request.getRequestURI())
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Triggered by @Validated on @RequestParam or @PathVariable constraints.
     * This is a different exception from MethodArgumentNotValidException — both must be handled.
     *
     * Example: a @Min(1) on a Long path variable produces ConstraintViolationException, not
     * MethodArgumentNotValidException.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(cv -> {
            // PropertyPath format: "methodName.paramName" — we want just "paramName"
            String propertyPath = cv.getPropertyPath().toString();
            String fieldName = propertyPath.contains(".")
                    ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
                    : propertyPath;
            fieldErrors.putIfAbsent(fieldName, cv.getMessage());
        });

        String traceId = traceId();
        log.debug("[{}] Constraint violation on {} {}: {}",
                traceId, request.getMethod(), request.getRequestURI(), fieldErrors);

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .errorCode(ErrorCode.INVALID_PARAMETER)
                .message("One or more request parameters are invalid — see fieldErrors for details")
                .path(request.getRequestURI())
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Triggered when a path variable or query parameter cannot be converted to the target type.
     * Example: GET /api/users/abc when id is declared as Long.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String typeName = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "unknown";
        String message = String.format(
                "Parameter '%s' must be a valid %s", ex.getName(), typeName);

        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PARAMETER, message, request, traceId());
    }

    /**
     * Triggered when the request body is missing, contains invalid JSON syntax,
     * or specifies an unknown enum value (e.g. severity: "EXTREME").
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String traceId = traceId();
        // Log the technical cause internally but do not expose it
        log.debug("[{}] Unreadable message on {} {}: {}",
                traceId, request.getMethod(), request.getRequestURI(), ex.getMessage());

        return build(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST,
                "Request body is missing, malformed, or contains an invalid value (check enum fields)",
                request, traceId);
    }

    // -------------------------------------------------------------------------
    // HTTP protocol exceptions
    // -------------------------------------------------------------------------

    /**
     * Triggered when the HTTP method is not supported on the matched endpoint.
     * Example: PUT /api/permissions when only GET and POST are mapped.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String message = String.format("HTTP method '%s' is not supported on this endpoint", ex.getMethod());
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED, message, request, traceId());
    }

    /**
     * Triggered when the Content-Type header does not match what the endpoint consumes.
     * Example: sending application/xml to an endpoint that only accepts application/json.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        String message = String.format(
                "Content-Type '%s' is not supported. Use 'application/json'",
                ex.getContentType());
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                message, request, traceId());
    }

    /**
     * Triggered when no handler is registered for the requested path.
     * Spring Boot 3.2+ uses NoResourceFoundException instead of NoHandlerFoundException.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        String message = String.format("No endpoint found for %s %s",
                request.getMethod(), request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, ErrorCode.ENDPOINT_NOT_FOUND, message, request, traceId());
    }

    // -------------------------------------------------------------------------
    // Database exceptions
    // -------------------------------------------------------------------------

    /**
     * Triggered by DB-level unique constraint violations or FK failures.
     *
     * This handles the race condition where two concurrent requests pass the
     * service-level duplicate check but one fails at the DB INSERT:
     *
     *   Thread A: existsByEmail("x") → false
     *   Thread B: existsByEmail("x") → false
     *   Thread A: INSERT ... → success
     *   Thread B: INSERT ... → DataIntegrityViolationException  ← caught here
     *
     * Also catches foreign key violations (e.g. deleting a Permission that is
     * referenced by a RolePermission row).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String traceId = traceId();
        // Log the technical cause for ops — never expose it to the client
        log.error("[{}] Data integrity violation on {} {}: {}",
                traceId, request.getMethod(), request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());

        return build(HttpStatus.CONFLICT, ErrorCode.DATA_INTEGRITY_VIOLATION,
                "Operation rejected: a database constraint was violated. " +
                "Check for duplicate values or existing references before retrying.",
                request, traceId);
    }

    // -------------------------------------------------------------------------
    // Fallback
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {

        String traceId = traceId();
        // Full stack trace in server logs — nothing internal goes to the client
        log.error("[{}] Unhandled exception on {} {}",
                traceId, request.getMethod(), request.getRequestURI(), ex);

        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred. Please contact support with traceId: " + traceId,
                request, traceId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            HttpServletRequest request,
            String traceId) {

        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Maps AccessRiskException subclasses to HTTP status codes.
     * Adding a new exception type only requires one line here.
     */
    private HttpStatus resolveStatus(AccessRiskException ex) {
        return switch (ex) {
            case ResourceNotFoundException ignored  -> HttpStatus.NOT_FOUND;
            case DuplicateResourceException ignored -> HttpStatus.CONFLICT;
            case BusinessException ignored          -> HttpStatus.BAD_REQUEST;
            default                                 -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String traceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
