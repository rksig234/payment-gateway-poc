package com.sigmoid.paymentgateway.retry.backoff;

import org.springframework.stereotype.Component;

import java.time.Duration;

/** Exponential backoff: attempt 1->1s, 2->2s, 3->4s, 4->8s; attempt 5 escalates to the DLQ. */
@Component
public class BackoffPolicy {

    public static final int MAX_ATTEMPTS = 5;

    public boolean exhausted(int attempt) {
        return attempt >= MAX_ATTEMPTS;
    }

    public Duration delayForAttempt(int attempt) {
        long seconds = (long) Math.pow(2, Math.max(0, attempt - 1));
        return Duration.ofSeconds(seconds);
    }
}
