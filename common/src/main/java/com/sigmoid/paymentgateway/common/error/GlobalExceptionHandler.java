package com.sigmoid.paymentgateway.common.error;

import com.sigmoid.paymentgateway.common.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Translates exceptions into the shared error envelope so every service returns
 * a consistent error shape with the current traceId.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        log.warn("api_error code={} message={}", ex.code(), ex.getMessage());
        return build(ex.code(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(ErrorCode.VALIDATION_ERROR, message.isBlank() ? "Validation failed" : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(ErrorCode.VALIDATION_ERROR, "Malformed or missing request body");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        log.error("unhandled_error", ex);
        return build(ErrorCode.INTERNAL_ERROR, "Unexpected error");
    }

    private ResponseEntity<ApiError> build(ErrorCode code, String message) {
        ApiError body = ApiError.of(code, message, TraceContext.currentTraceId());
        return ResponseEntity.status(code.status()).body(body);
    }
}
