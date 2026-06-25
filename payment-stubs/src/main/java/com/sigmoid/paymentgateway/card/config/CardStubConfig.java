package com.sigmoid.paymentgateway.card.config;

import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Runtime-tunable config knobs for the Card stub (deck: timeoutRate 0-15%,
 * challengeRate 0-30%). {@code timeoutMs} controls how long a simulated timeout
 * hangs before failing — set above the breaker's slowCallDurationThreshold (2s)
 * to exercise slow-call detection in Module A.
 */
@Component
@ConfigurationProperties(prefix = "stub.card")
public class CardStubConfig {

    /** Probability (0-100%) a charge times out with PROVIDER_TIMEOUT. */
    private volatile int timeoutRate = 0;

    /** Probability (0-100%) a charge triggers a 3DS challenge. */
    private volatile int challengeRate = 0;

    /** How long a simulated timeout hangs before failing, in milliseconds. */
    private volatile int timeoutMs = 2500;

    public int getTimeoutRate() {
        return timeoutRate;
    }

    public void setTimeoutRate(int timeoutRate) {
        this.timeoutRate = timeoutRate;
    }

    public int getChallengeRate() {
        return challengeRate;
    }

    public void setChallengeRate(int challengeRate) {
        this.challengeRate = challengeRate;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /** Validated runtime update from the admin endpoint. */
    public synchronized void apply(Integer newTimeoutRate, Integer newChallengeRate, Integer newTimeoutMs) {
        if (newTimeoutRate != null) {
            if (newTimeoutRate < 0 || newTimeoutRate > 100) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "timeoutRate must be between 0 and 100");
            }
            this.timeoutRate = newTimeoutRate;
        }
        if (newChallengeRate != null) {
            if (newChallengeRate < 0 || newChallengeRate > 100) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "challengeRate must be between 0 and 100");
            }
            this.challengeRate = newChallengeRate;
        }
        if (newTimeoutMs != null) {
            if (newTimeoutMs < 0 || newTimeoutMs > 30_000) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "timeoutMs must be between 0 and 30000");
            }
            this.timeoutMs = newTimeoutMs;
        }
    }
}
