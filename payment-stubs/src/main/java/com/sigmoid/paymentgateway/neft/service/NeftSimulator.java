package com.sigmoid.paymentgateway.neft.service;

import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import com.sigmoid.paymentgateway.common.model.PaymentStatus;
import com.sigmoid.paymentgateway.common.obs.MetricNames;
import com.sigmoid.paymentgateway.common.trace.TraceConstants;
import com.sigmoid.paymentgateway.common.util.Ids;
import com.sigmoid.paymentgateway.neft.config.NeftStubConfig;
import com.sigmoid.paymentgateway.neft.dto.NeftStatusResponse;
import com.sigmoid.paymentgateway.neft.dto.NeftTransferRequest;
import com.sigmoid.paymentgateway.neft.dto.NeftTransferResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates the NEFT rail's batch-style processing: a transfer is accepted
 * immediately (202 PROCESSING) and parked in an in-memory store, then settled to
 * SUCCESS on a periodic batch tick once its delay has elapsed.
 */
@Service
public class NeftSimulator {

    private static final Logger log = LoggerFactory.getLogger(NeftSimulator.class);
    private static final String PROVIDER = "NEFT";

    private final NeftStubConfig config;
    private final MeterRegistry metrics;
    private final Map<String, Transfer> store = new ConcurrentHashMap<>();

    public NeftSimulator(NeftStubConfig config, MeterRegistry metrics) {
        this.config = config;
        this.metrics = metrics;
    }

    public NeftTransferResponse accept(NeftTransferRequest request) {
        MDC.put(TraceConstants.MDC_REF_ID, request.refId());
        Instant now = Instant.now();
        Instant dueAt = now.plusSeconds(config.effectiveDelaySeconds());
        Transfer transfer = new Transfer(request.refId(), request.amount(), request.ifsc(), now, dueAt);
        store.put(request.refId(), transfer);

        metrics.counter(MetricNames.REQUESTS,
                MetricNames.TAG_PROVIDER, PROVIDER,
                MetricNames.TAG_STATUS, "ACCEPTED").increment();
        log.info("neft_transfer_accepted refId={} amount={} ifsc={} dueAt={}",
                request.refId(), request.amount(), request.ifsc(), dueAt);

        return new NeftTransferResponse(PaymentStatus.PROCESSING.name(), config.ackEtaLabel(), dueAt);
    }

    public NeftStatusResponse status(String refId) {
        Transfer transfer = store.get(refId);
        if (transfer == null) {
            throw new ApiException(ErrorCode.PAYMENT_NOT_FOUND, "No NEFT transfer for refId " + refId);
        }
        return new NeftStatusResponse(
                transfer.refId, transfer.status.name(), transfer.utr, transfer.acceptedAt, transfer.settledAt);
    }

    /**
     * Batch settler: scans parked transfers and settles those whose delay has
     * elapsed. Interval is configurable so tests can run fast.
     */
    @Scheduled(fixedDelayString = "${stub.neft.batch-scan-ms:1000}")
    public void settleDueTransfers() {
        Instant now = Instant.now();
        for (Transfer transfer : store.values()) {
            if (transfer.status == PaymentStatus.PROCESSING && !transfer.dueAt.isAfter(now)) {
                transfer.utr = Ids.utr();
                transfer.settledAt = now;
                transfer.status = PaymentStatus.SUCCESS;
                metrics.counter(MetricNames.REQUESTS,
                        MetricNames.TAG_PROVIDER, PROVIDER,
                        MetricNames.TAG_STATUS, "SUCCESS").increment();
                log.info("neft_transfer_settled refId={} utr={}", transfer.refId, transfer.utr);
            }
        }
    }

    /** Mutable in-memory transfer record. */
    private static final class Transfer {
        private final String refId;
        private final BigDecimal amount;
        private final String ifsc;
        private final Instant acceptedAt;
        private final Instant dueAt;
        private volatile PaymentStatus status = PaymentStatus.PROCESSING;
        private volatile String utr;
        private volatile Instant settledAt;

        private Transfer(String refId, BigDecimal amount, String ifsc, Instant acceptedAt, Instant dueAt) {
            this.refId = refId;
            this.amount = amount;
            this.ifsc = ifsc;
            this.acceptedAt = acceptedAt;
            this.dueAt = dueAt;
        }
    }
}
