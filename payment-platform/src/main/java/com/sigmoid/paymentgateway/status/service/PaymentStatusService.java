package com.sigmoid.paymentgateway.status.service;

import com.sigmoid.paymentgateway.common.event.PaymentFailedEvent;
import com.sigmoid.paymentgateway.common.event.PaymentInitiatedEvent;
import com.sigmoid.paymentgateway.common.event.PaymentRetryEvent;
import com.sigmoid.paymentgateway.common.event.PaymentSuccessEvent;
import com.sigmoid.paymentgateway.common.model.PaymentStatus;
import com.sigmoid.paymentgateway.status.domain.PaymentEntity;
import com.sigmoid.paymentgateway.status.domain.PaymentTimelineEntity;
import com.sigmoid.paymentgateway.status.repo.PaymentRepository;
import com.sigmoid.paymentgateway.status.repo.PaymentTimelineRepository;
import com.sigmoid.paymentgateway.status.statemachine.PaymentStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Applies payment.* events to the durable state machine and records every
 * transition in the timeline. Transitions are validated so out-of-order or
 * duplicate events can't corrupt state.
 */
@Service
public class PaymentStatusService {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusService.class);

    private final PaymentRepository payments;
    private final PaymentTimelineRepository timeline;
    private final PaymentStateMachine stateMachine;

    public PaymentStatusService(PaymentRepository payments, PaymentTimelineRepository timeline,
                                PaymentStateMachine stateMachine) {
        this.payments = payments;
        this.timeline = timeline;
        this.stateMachine = stateMachine;
    }

    @Transactional
    public void onInitiated(PaymentInitiatedEvent e) {
        if (payments.existsById(e.paymentId())) {
            return; // idempotent on redelivery
        }
        Instant now = Instant.now();
        PaymentEntity p = new PaymentEntity(e.paymentId());
        p.setCustomerId(e.customerId());
        p.setAmount(e.amount());
        p.setCurrency(e.currency());
        p.setRail(e.rail());
        p.setStatus(PaymentStatus.INITIATED);
        p.setTraceId(e.traceId());
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        payments.save(p);
        addTimeline(e.paymentId(), PaymentStatus.INITIATED, now);
        log.info("status_initiated paymentId={}", e.paymentId());
    }

    @Transactional
    public void onSuccess(PaymentSuccessEvent e) {
        PaymentEntity p = getOrCreate(e.paymentId(), e.rail() == null ? null : e.rail());
        ensureProcessing(p);
        if (transition(p, PaymentStatus.SUCCESS)) {
            p.setProvider(e.rail() == null ? p.getProvider() : e.rail().name());
            persist(p, PaymentStatus.SUCCESS);
            log.info("status_success paymentId={}", e.paymentId());
        }
    }

    @Transactional
    public void onFailed(PaymentFailedEvent e) {
        PaymentEntity p = getOrCreate(e.paymentId(), e.rail());
        ensureProcessing(p);
        if (transition(p, PaymentStatus.FAILED)) {
            persist(p, PaymentStatus.FAILED);
            log.info("status_failed paymentId={} reason={}", e.paymentId(), e.errorReason());
        }
    }

    @Transactional
    public void onRetry(PaymentRetryEvent e) {
        PaymentEntity p = getOrCreate(e.paymentId(), e.provider());
        p.setRetries(e.retryCount());
        p.setUpdatedAt(Instant.now());
        payments.save(p);
        log.info("status_retry paymentId={} retryCount={}", e.paymentId(), e.retryCount());
    }

    private PaymentEntity getOrCreate(String paymentId, com.sigmoid.paymentgateway.common.model.Rail rail) {
        return payments.findById(paymentId).orElseGet(() -> {
            Instant now = Instant.now();
            PaymentEntity p = new PaymentEntity(paymentId);
            p.setStatus(PaymentStatus.INITIATED);
            p.setRail(rail);
            p.setCreatedAt(now);
            p.setUpdatedAt(now);
            payments.save(p);
            addTimeline(paymentId, PaymentStatus.INITIATED, now);
            return p;
        });
    }

    private void ensureProcessing(PaymentEntity p) {
        if (p.getStatus() == PaymentStatus.INITIATED && transition(p, PaymentStatus.PROCESSING)) {
            persist(p, PaymentStatus.PROCESSING);
        }
    }

    private boolean transition(PaymentEntity p, PaymentStatus to) {
        if (!stateMachine.canTransition(p.getStatus(), to)) {
            log.warn("status_transition_rejected paymentId={} from={} to={}", p.getPaymentId(), p.getStatus(), to);
            return false;
        }
        p.setStatus(to);
        return true;
    }

    private void persist(PaymentEntity p, PaymentStatus state) {
        Instant now = Instant.now();
        p.setUpdatedAt(now);
        payments.save(p);
        addTimeline(p.getPaymentId(), state, now);
    }

    private void addTimeline(String paymentId, PaymentStatus state, Instant at) {
        timeline.save(new PaymentTimelineEntity(paymentId, state, at));
    }
}
