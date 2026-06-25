package com.sigmoid.paymentgateway.upi.config;

import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Runtime-tunable config knobs for the UPI stub (deck: latencyMs 200-800,
 * failureRate 0-100%). Values are seeded from application.yml and can be changed
 * live via the admin endpoint so a demo can open/close the circuit breaker
 * without a restart. Fields are volatile for safe cross-thread visibility.
 */
@Component
@ConfigurationProperties(prefix = "stub.upi")
public class UpiStubConfig {

    /** Simulated processing latency in milliseconds. */
    private volatile int latencyMs = 300;

    /** Probability (0-100%) that a debit fails with PROVIDER_UNAVAILABLE. */
    private volatile int failureRate = 0;

    public int getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(int latencyMs) {
        this.latencyMs = latencyMs;
    }

    public int getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(int failureRate) {
        this.failureRate = failureRate;
    }

    /** Validated runtime update applied from the admin endpoint. */
    public synchronized void apply(Integer newLatencyMs, Integer newFailureRate) {
        if (newLatencyMs != null) {
            if (newLatencyMs < 0 || newLatencyMs > 10_000) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "latencyMs must be between 0 and 10000");
            }
            this.latencyMs = newLatencyMs;
        }
        if (newFailureRate != null) {
            if (newFailureRate < 0 || newFailureRate > 100) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "failureRate must be between 0 and 100");
            }
            this.failureRate = newFailureRate;
        }
    }
}
