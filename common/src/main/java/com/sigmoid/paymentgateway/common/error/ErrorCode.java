package com.sigmoid.paymentgateway.common.error;

import org.springframework.http.HttpStatus;

/**
 * Standard error codes from the Build Design deck (shared schemas slide), plus
 * PROVIDER_TIMEOUT to model the Card stub's timeout scenario explicitly.
 */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT),
    PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    PROVIDER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
