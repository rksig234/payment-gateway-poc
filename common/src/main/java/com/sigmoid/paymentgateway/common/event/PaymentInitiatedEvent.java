package com.sigmoid.paymentgateway.common.event;

import com.sigmoid.paymentgateway.common.model.Rail;

import java.math.BigDecimal;
import java.time.Instant;

/** payment.initiated — Gateway emits this when a new payment is accepted. */
public record PaymentInitiatedEvent(
        String paymentId,
        String customerId,
        BigDecimal amount,
        String currency,
        Rail rail,
        String traceId,
        Instant occurredAt
) {
}
