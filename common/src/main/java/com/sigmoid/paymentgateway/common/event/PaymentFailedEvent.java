package com.sigmoid.paymentgateway.common.event;

import com.sigmoid.paymentgateway.common.model.Rail;

import java.math.BigDecimal;
import java.time.Instant;

/** payment.failed — Gateway emits this when a provider call fails terminally for the attempt. */
public record PaymentFailedEvent(
        String paymentId,
        Rail rail,
        BigDecimal amount,
        String errorReason,
        String traceId,
        Instant occurredAt
) {
}
