package com.sigmoid.paymentgateway.gateway.service;

import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import com.sigmoid.paymentgateway.common.event.PaymentFailedEvent;
import com.sigmoid.paymentgateway.common.event.PaymentInitiatedEvent;
import com.sigmoid.paymentgateway.common.event.PaymentRetryEvent;
import com.sigmoid.paymentgateway.common.event.PaymentSuccessEvent;
import com.sigmoid.paymentgateway.common.model.PaymentStatus;
import com.sigmoid.paymentgateway.common.util.Ids;
import com.sigmoid.paymentgateway.gateway.api.dto.InitiatePaymentRequest;
import com.sigmoid.paymentgateway.gateway.api.dto.PaymentResponse;
import com.sigmoid.paymentgateway.gateway.events.PaymentEventPublisher;
import com.sigmoid.paymentgateway.gateway.idempotency.IdempotencyService;
import com.sigmoid.paymentgateway.gateway.provider.ProviderResult;
import com.sigmoid.paymentgateway.gateway.routing.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Orchestrates a payment: idempotency check/claim, emit {@code payment.initiated},
 * route to a provider (with breaker + fallback), then emit {@code payment.success}
 * or {@code payment.failed} + {@code payment.retry}. The synchronous response is
 * always PROCESSING — terminal state is owned by the Payment Status Service (Module C).
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final IdempotencyService idempotency;
    private final RoutingService routing;
    private final PaymentEventPublisher events;

    public PaymentService(IdempotencyService idempotency, RoutingService routing, PaymentEventPublisher events) {
        this.idempotency = idempotency;
        this.routing = routing;
        this.events = events;
    }

    public InitiateResult initiate(InitiatePaymentRequest req, String idempotencyKey, String traceId) {
        PaymentResponse cached = idempotency.cachedResponse(idempotencyKey);
        if (cached != null) {
            log.info("idempotent_replay key={} paymentId={}", idempotencyKey, cached.paymentId());
            return new InitiateResult(cached, true);
        }
        if (!idempotency.claim(idempotencyKey)) {
            // Lost the race or in-flight: a duplicate is being processed right now.
            PaymentResponse raced = idempotency.cachedResponse(idempotencyKey);
            if (raced != null) {
                return new InitiateResult(raced, true);
            }
            throw new ApiException(ErrorCode.IDEMPOTENCY_CONFLICT, "Request already in progress");
        }

        String paymentId = Ids.paymentId();
        Instant now = Instant.now();
        events.initiated(new PaymentInitiatedEvent(
                paymentId, req.customerId(), req.amount(), req.currency(), req.rail(), traceId, now));

        try {
            ProviderResult pr = routing.route(req, paymentId, traceId);
            PaymentResponse response = new PaymentResponse(
                    paymentId, PaymentStatus.PROCESSING.name(), pr.provider().name(), 0, traceId);
            idempotency.store(idempotencyKey, paymentId, response);
            events.success(new PaymentSuccessEvent(
                    paymentId, pr.provider(), req.amount(), pr.settlementRef(), traceId, Instant.now()));
            log.info("payment_accepted paymentId={} provider={}", paymentId, pr.provider());
            return new InitiateResult(response, false);
        } catch (ApiException ex) {
            Instant failedAt = Instant.now();
            events.failed(new PaymentFailedEvent(
                    paymentId, req.rail(), req.amount(), ex.getMessage(), traceId, failedAt));
            events.retry(new PaymentRetryEvent(
                    paymentId, req.rail(), 1, ex.code().name(), failedAt.plusSeconds(1), traceId, failedAt));
            log.warn("payment_failed paymentId={} reason={}", paymentId, ex.getMessage());
            throw ex;
        }
    }

    public PaymentResponse status(String paymentId) {
        return idempotency.byPaymentId(paymentId);
    }

    /** Result of an initiate call: the response plus whether it was an idempotent replay. */
    public record InitiateResult(PaymentResponse response, boolean replayed) {
    }
}
