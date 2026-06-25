package com.sigmoid.paymentgateway.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response for POST /v1/payments and GET /v1/payments/{id}.
 * On first accept: 201 with status PROCESSING. On idempotent replay: 200 (same body).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        String paymentId,
        String status,
        String provider,
        Integer retries,
        String traceId
) {
}
