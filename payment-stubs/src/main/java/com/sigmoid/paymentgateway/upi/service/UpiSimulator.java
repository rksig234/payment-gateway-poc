package com.sigmoid.paymentgateway.upi.service;

import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import com.sigmoid.paymentgateway.common.obs.MetricNames;
import com.sigmoid.paymentgateway.common.trace.TraceConstants;
import com.sigmoid.paymentgateway.common.util.Ids;
import com.sigmoid.paymentgateway.upi.config.UpiStubConfig;
import com.sigmoid.paymentgateway.upi.dto.UpiDebitRequest;
import com.sigmoid.paymentgateway.upi.dto.UpiDebitResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates the UPI rail. Applies the configured latency, then either succeeds
 * with a generated UTR or fails with PROVIDER_UNAVAILABLE based on failureRate.
 * Emits payment_requests_total{provider,status} and payment_latency_seconds{provider}.
 */
@Service
public class UpiSimulator {

    private static final Logger log = LoggerFactory.getLogger(UpiSimulator.class);
    private static final String PROVIDER = "UPI";

    private final UpiStubConfig config;
    private final MeterRegistry metrics;

    public UpiSimulator(UpiStubConfig config, MeterRegistry metrics) {
        this.config = config;
        this.metrics = metrics;
    }

    public UpiDebitResponse debit(UpiDebitRequest request) {
        MDC.put(TraceConstants.MDC_REF_ID, request.refId());
        Timer.Sample sample = Timer.start(metrics);
        String outcome = "SUCCESS";
        try {
            sleep(config.getLatencyMs());
            if (shouldFail()) {
                outcome = "FAILED";
                log.warn("upi_debit_failed refId={} amount={} reason=SIMULATED_FAILURE", request.refId(), request.amount());
                throw new ApiException(ErrorCode.PROVIDER_UNAVAILABLE, "UPI rail unavailable (simulated)");
            }
            String utr = Ids.utr();
            log.info("upi_debit_success refId={} amount={} utr={}", request.refId(), request.amount(), utr);
            return new UpiDebitResponse("SUCCESS", utr);
        } finally {
            sample.stop(metrics.timer(MetricNames.LATENCY, MetricNames.TAG_PROVIDER, PROVIDER));
            metrics.counter(MetricNames.REQUESTS,
                    MetricNames.TAG_PROVIDER, PROVIDER,
                    MetricNames.TAG_STATUS, outcome).increment();
        }
    }

    private boolean shouldFail() {
        int rate = config.getFailureRate();
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
