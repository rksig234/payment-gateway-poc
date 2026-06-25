package com.sigmoid.paymentgateway.status.statemachine;

import org.junit.jupiter.api.Test;

import static com.sigmoid.paymentgateway.common.model.PaymentStatus.FAILED;
import static com.sigmoid.paymentgateway.common.model.PaymentStatus.INITIATED;
import static com.sigmoid.paymentgateway.common.model.PaymentStatus.PROCESSING;
import static com.sigmoid.paymentgateway.common.model.PaymentStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentStateMachineTest {

    private final PaymentStateMachine sm = new PaymentStateMachine();

    @Test
    void allowsTheHappyPath() {
        assertThat(sm.canTransition(INITIATED, PROCESSING)).isTrue();
        assertThat(sm.canTransition(PROCESSING, SUCCESS)).isTrue();
        assertThat(sm.canTransition(PROCESSING, FAILED)).isTrue();
    }

    @Test
    void rejectsInvalidJumps() {
        assertThat(sm.canTransition(INITIATED, SUCCESS)).isFalse();
        assertThat(sm.canTransition(SUCCESS, FAILED)).isFalse();
        assertThat(sm.canTransition(FAILED, PROCESSING)).isFalse();
    }
}
