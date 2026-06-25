package com.sigmoid.paymentgateway.retry.backoff;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffPolicyTest {

    private final BackoffPolicy policy = new BackoffPolicy();

    @Test
    void backoffDoublesPerAttempt() {
        assertThat(policy.delayForAttempt(1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(policy.delayForAttempt(2)).isEqualTo(Duration.ofSeconds(2));
        assertThat(policy.delayForAttempt(3)).isEqualTo(Duration.ofSeconds(4));
        assertThat(policy.delayForAttempt(4)).isEqualTo(Duration.ofSeconds(8));
    }

    @Test
    void exhaustsAtFifthAttempt() {
        assertThat(policy.exhausted(4)).isFalse();
        assertThat(policy.exhausted(5)).isTrue();
    }
}
