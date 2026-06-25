package com.sigmoid.paymentgateway.status.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigmoid.paymentgateway.common.event.PaymentFailedEvent;
import com.sigmoid.paymentgateway.common.event.PaymentInitiatedEvent;
import com.sigmoid.paymentgateway.common.event.PaymentRetryEvent;
import com.sigmoid.paymentgateway.common.event.PaymentSuccessEvent;
import com.sigmoid.paymentgateway.common.kafka.Topics;
import com.sigmoid.paymentgateway.common.trace.TraceConstants;
import com.sigmoid.paymentgateway.status.service.PaymentStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes the four payment topics (JSON as String) and applies them to the
 * status service. The traceId rides in a Kafka header and is put on the MDC so
 * the status service's logs stay on the same trace.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final PaymentStatusService service;

    public PaymentEventConsumer(ObjectMapper objectMapper, PaymentStatusService service) {
        this.objectMapper = objectMapper;
        this.service = service;
    }

    @KafkaListener(topics = Topics.PAYMENT_INITIATED, groupId = "status-service")
    public void onInitiated(@Payload String payload,
                            @Header(name = TraceConstants.TRACE_HEADER, required = false) byte[] trace) {
        withTrace(trace, () -> service.onInitiated(parse(payload, PaymentInitiatedEvent.class)));
    }

    @KafkaListener(topics = Topics.PAYMENT_SUCCESS, groupId = "status-service")
    public void onSuccess(@Payload String payload,
                          @Header(name = TraceConstants.TRACE_HEADER, required = false) byte[] trace) {
        withTrace(trace, () -> service.onSuccess(parse(payload, PaymentSuccessEvent.class)));
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "status-service")
    public void onFailed(@Payload String payload,
                         @Header(name = TraceConstants.TRACE_HEADER, required = false) byte[] trace) {
        withTrace(trace, () -> service.onFailed(parse(payload, PaymentFailedEvent.class)));
    }

    @KafkaListener(topics = Topics.PAYMENT_RETRY, groupId = "status-service")
    public void onRetry(@Payload String payload,
                        @Header(name = TraceConstants.TRACE_HEADER, required = false) byte[] trace) {
        withTrace(trace, () -> service.onRetry(parse(payload, PaymentRetryEvent.class)));
    }

    private <T> T parse(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Bad " + type.getSimpleName() + " payload: " + e.getMessage(), e);
        }
    }

    private void withTrace(byte[] trace, Runnable action) {
        if (trace != null) {
            MDC.put(TraceConstants.MDC_TRACE_ID, new String(trace, java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            action.run();
        } catch (Exception ex) {
            log.error("event_handling_failed", ex);
        } finally {
            MDC.remove(TraceConstants.MDC_TRACE_ID);
        }
    }
}
