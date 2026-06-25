package com.sigmoid.paymentgateway.status.statemachine;

import com.sigmoid.paymentgateway.common.model.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Allowed transitions: INITIATED -> PROCESSING -> SUCCESS | FAILED.
 * Terminal states accept no further transitions. Used to reject out-of-order or
 * invalid events instead of corrupting state.
 */
@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
            PaymentStatus.INITIATED, EnumSet.of(PaymentStatus.PROCESSING),
            PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.SUCCESS, PaymentStatus.FAILED),
            PaymentStatus.SUCCESS, EnumSet.noneOf(PaymentStatus.class),
            PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class)
    );

    public boolean canTransition(PaymentStatus from, PaymentStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
}
