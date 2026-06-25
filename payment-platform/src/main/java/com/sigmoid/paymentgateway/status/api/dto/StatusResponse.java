package com.sigmoid.paymentgateway.status.api.dto;

import java.time.Instant;

/** GET /v1/payments/{id} body. */
public record StatusResponse(
        String paymentId,
        String status,
        String provider,
        int retries,
        Instant updatedAt
) {
}
