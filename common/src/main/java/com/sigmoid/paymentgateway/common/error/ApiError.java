package com.sigmoid.paymentgateway.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Error envelope from the shared schemas slide:
 * { "error": { "code", "message", "traceId", "timestamp" } }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Detail error) {

    public record Detail(String code, String message, String traceId, Instant timestamp) {
    }

    public static ApiError of(ErrorCode code, String message, String traceId) {
        return new ApiError(new Detail(code.name(), message, traceId, Instant.now()));
    }
}
