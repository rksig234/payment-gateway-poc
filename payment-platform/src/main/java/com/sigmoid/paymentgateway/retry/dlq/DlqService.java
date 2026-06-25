package com.sigmoid.paymentgateway.retry.dlq;

import com.sigmoid.paymentgateway.common.event.PaymentRetryEvent;
import com.sigmoid.paymentgateway.retry.domain.DlqRecordEntity;
import com.sigmoid.paymentgateway.retry.events.RetryEventPublisher;
import com.sigmoid.paymentgateway.retry.repo.DlqRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Persists exhausted retries to dlq_records and publishes them to the payment.dlq topic. */
@Service
public class DlqService {

    private static final Logger log = LoggerFactory.getLogger(DlqService.class);

    private final DlqRecordRepository repo;
    private final RetryEventPublisher publisher;

    public DlqService(DlqRecordRepository repo, RetryEventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    @Transactional
    public void moveToDlq(PaymentRetryEvent event, String rawPayload) {
        repo.save(new DlqRecordEntity(
                event.paymentId(), rawPayload, event.lastError(), event.retryCount(), Instant.now()));
        publisher.publishToDlq(event.paymentId(), event.traceId(), event);
        log.warn("dlq_escalated paymentId={} attempts={} error={}",
                event.paymentId(), event.retryCount(), event.lastError());
    }
}
