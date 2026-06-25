package com.sigmoid.paymentgateway.neft.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** GET /internal/neft/transfer/{refId} body. utr/settledAt appear once settled. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NeftStatusResponse(
        String refId,
        String status,
        String utr,
        Instant acceptedAt,
        Instant settledAt
) {
}
