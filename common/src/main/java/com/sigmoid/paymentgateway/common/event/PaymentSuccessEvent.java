package com.sigmoid.paymentgateway.common.event;

import com.sigmoid.paymentgateway.common.model.Rail;

import java.math.BigDecimal;
import java.time.Instant;

/** payment.success — matches the deck schema: paymentId, rail, amount, settlementRef, traceId, occurredAt. */
public record PaymentSuccessEvent(
        String paymentId,
        Rail rail,
        BigDecimal amount,
        String settlementRef,
        String traceId,
        Instant occurredAt
) {
}
