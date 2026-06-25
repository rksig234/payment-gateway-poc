package com.sigmoid.paymentgateway.retry.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigmoid.paymentgateway.common.event.PaymentRetryEvent;
import com.sigmoid.paymentgateway.common.kafka.Topics;
import com.sigmoid.paymentgateway.common.trace.TraceConstants;
import com.sigmoid.paymentgateway.retry.backoff.BackoffPolicy;
import com.sigmoid.paymentgateway.retry.dlq.DlqService;
import com.sigmoid.paymentgateway.retry.events.RetryEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Consumes payment.retry. For attempts below the cap it schedules the next
 * payment.retry after the backoff delay (non-blocking); on the final attempt it
 * escalates to the DLQ. The escalation chain is: 1s -> 2s -> 4s -> 8s -> DLQ.
 */
@Component
public class RetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(RetryConsumer.class);

    private final ObjectMapper objectMapper;
    private final BackoffPolicy backoff;
    private final TaskScheduler scheduler;
    private final RetryEventPublisher publisher;
    private final DlqService dlqService;

    public RetryConsumer(ObjectMapper objectMapper, BackoffPolicy backoff, TaskScheduler scheduler,
                         RetryEventPublisher publisher, DlqService dlqService) {
        this.objectMapper = objectMapper;
        this.backoff = backoff;
        this.scheduler = scheduler;
        this.publisher = publisher;
        this.dlqService = dlqService;
    }

    @KafkaListener(topics = Topics.PAYMENT_RETRY, groupId = "retry-service")
    public void onRetry(@Payload String payload,
                        @Header(name = TraceConstants.TRACE_HEADER, required = false) byte[] trace) {
        if (trace != null) {
            MDC.put(TraceConstants.MDC_TRACE_ID, new String(trace, StandardCharsets.UTF_8));
        }
        try {
            PaymentRetryEvent event = objectMapper.readValue(payload, PaymentRetryEvent.class);
            int attempt = event.retryCount();

            if (backoff.exhausted(attempt)) {
                dlqService.moveToDlq(event, payload);
                return;
            }

            Duration delay = backoff.delayForAttempt(attempt);
            Instant nextAttemptAt = Instant.now().plus(delay);
            PaymentRetryEvent next = new PaymentRetryEvent(
                    event.paymentId(), event.provider(), attempt + 1, event.lastError(),
                    nextAttemptAt, event.traceId(), Instant.now());

            scheduler.schedule(() -> publisher.publishRetry(next), nextAttemptAt);
            log.info("retry_scheduled paymentId={} attempt={} delay={}s nextAt={}",
                    event.paymentId(), attempt, delay.toSeconds(), nextAttemptAt);
        } catch (Exception ex) {
            log.error("retry_handling_failed", ex);
        } finally {
            MDC.remove(TraceConstants.MDC_TRACE_ID);
        }
    }
}
