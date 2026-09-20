package io.aygh.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * Unified API response envelope used for every controller response —
 * both successful payloads and structured error responses.
 *
 * <pre>
 * {
 *   "success": true,
 *   "status":  200,
 *   "message": "OK",
 *   "data":    { ... },          // null on error
 *   "errors":  [],               // populated on validation failure
 *   "timestamp": "2026-..."
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        List<FieldError> errors,
        Instant timestamp
) {
    // ── Nested error type ──────────────────────────────────────────────────
    public record FieldError(String field, String message) {
    }

    // ── Success factories ──────────────────────────────────────────────────

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), "OK", data, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), message, data, null, Instant.now());
    }

    /** A success with nothing to return but the message — a delete, a password reset. */
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), message, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), "Created", data, null, Instant.now());
    }

    public static <T> ApiResponse<T> noContent() {
        return new ApiResponse<>(true, HttpStatus.NO_CONTENT.value(), "No Content", null, null, Instant.now());
    }

    // ── Error factories ────────────────────────────────────────────────────

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return new ApiResponse<>(false, status.value(), message, null, List.of(), Instant.now());
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, List<FieldError> errors) {
        return new ApiResponse<>(false, status.value(), message, null, errors, Instant.now());
    }

    /**
     * A failure whose payload is the explanation: a bulk import's row-by-row
     * report, where the useful part is the body rather than the status. The
     * other error factories null {@code data} out, which for these would throw
     * away the only thing the caller needs to act on.
     */
    public static <T> ApiResponse<T> failed(HttpStatus status, String message, T data) {
        return new ApiResponse<>(false, status.value(), message, data, List.of(), Instant.now());
    }
}
