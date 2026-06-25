package com.sigmoid.paymentgateway.common.event;

import com.sigmoid.paymentgateway.common.model.Rail;

import java.time.Instant;

/** payment.retry — schema from the deck: paymentId, provider, retryCount, lastError, nextAttemptAt. */
public record PaymentRetryEvent(
        String paymentId,
        Rail provider,
        int retryCount,
        String lastError,
        Instant nextAttemptAt,
        String traceId,
        Instant occurredAt
) {
}
