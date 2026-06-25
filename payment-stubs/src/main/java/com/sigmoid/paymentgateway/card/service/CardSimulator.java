package com.sigmoid.paymentgateway.card.service;

import com.sigmoid.paymentgateway.card.config.CardStubConfig;
import com.sigmoid.paymentgateway.card.dto.CardChargeRequest;
import com.sigmoid.paymentgateway.card.dto.CardChargeResponse;
import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import com.sigmoid.paymentgateway.common.obs.MetricNames;
import com.sigmoid.paymentgateway.common.trace.TraceConstants;
import com.sigmoid.paymentgateway.common.util.Ids;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a card network. A charge can (a) time out, (b) require a 3DS
 * challenge that is completed via a second call, or (c) pass frictionlessly.
 * Outcome is driven by timeoutRate / challengeRate (timeout takes precedence).
 */
@Service
public class CardSimulator {

    private static final Logger log = LoggerFactory.getLogger(CardSimulator.class);
    private static final String PROVIDER = "CARD";

    private final CardStubConfig config;
    private final MeterRegistry metrics;
    /** Outstanding 3DS challenges: challengeId -> refId. */
    private final Map<String, String> pendingChallenges = new ConcurrentHashMap<>();

    public CardSimulator(CardStubConfig config, MeterRegistry metrics) {
        this.config = config;
        this.metrics = metrics;
    }

    public CardChargeResponse charge(CardChargeRequest request) {
        MDC.put(TraceConstants.MDC_REF_ID, request.refId());
        Timer.Sample sample = Timer.start(metrics);
        String outcome = "SUCCESS";
        try {
            if (rolls(config.getTimeoutRate())) {
                outcome = "TIMEOUT";
                sleep(config.getTimeoutMs());
                log.warn("card_charge_timeout refId={} amount={}", request.refId(), request.amount());
                throw new ApiException(ErrorCode.PROVIDER_TIMEOUT, "Card network timeout (simulated)");
            }
            if (rolls(config.getChallengeRate())) {
                outcome = "CHALLENGE";
                String challengeId = Ids.challengeId();
                pendingChallenges.put(challengeId, request.refId());
                log.info("card_charge_challenge refId={} challengeId={}", request.refId(), challengeId);
                return new CardChargeResponse("REQUIRES_CHALLENGE", "3DS_REQUIRED", challengeId, request.refId());
            }
            log.info("card_charge_success refId={} amount={}", request.refId(), request.amount());
            return new CardChargeResponse("SUCCESS", "3DS_PASSED", null, request.refId());
        } finally {
            sample.stop(metrics.timer(MetricNames.LATENCY, MetricNames.TAG_PROVIDER, PROVIDER));
            metrics.counter(MetricNames.REQUESTS,
                    MetricNames.TAG_PROVIDER, PROVIDER,
                    MetricNames.TAG_STATUS, outcome).increment();
        }
    }

    public CardChargeResponse completeChallenge(String challengeId) {
        String refId = pendingChallenges.remove(challengeId);
        if (refId == null) {
            throw new ApiException(ErrorCode.PAYMENT_NOT_FOUND, "No pending 3DS challenge " + challengeId);
        }
        MDC.put(TraceConstants.MDC_REF_ID, refId);
        metrics.counter(MetricNames.REQUESTS,
                MetricNames.TAG_PROVIDER, PROVIDER,
                MetricNames.TAG_STATUS, "SUCCESS").increment();
        log.info("card_challenge_completed refId={} challengeId={}", refId, challengeId);
        return new CardChargeResponse("SUCCESS", "3DS_PASSED", null, refId);
    }

    private boolean rolls(int rate) {
        return rate > 0 && ThreadLocalRandom.current().nextInt(100) < rate;
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
