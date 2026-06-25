package com.sigmoid.paymentgateway.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigmoid.paymentgateway.common.event.PaymentFailedEvent;
import com.sigmoid.paymentgateway.common.event.PaymentSuccessEvent;
import com.sigmoid.paymentgateway.common.kafka.Topics;
import com.sigmoid.paymentgateway.common.trace.TraceConstants;
import com.sigmoid.paymentgateway.notification.dispatch.NotificationDispatcher;
import com.sigmoid.paymentgateway.notification.model.NotificationMessage;
import com.sigmoid.paymentgateway.notification.sns.SnsPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Consumes payment.success / payment.failed and delivers the outcome via channels + SNS. */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationDispatcher dispatcher;
    private final SnsPublisher sns;

    public NotificationConsumer(ObjectMapper objectMapper, NotificationDispatcher dispatcher, SnsPublisher sns) {
        this.objectMapper = objectMapper;
        this.dispatcher = dispatcher;
        this.sns = sns;
    }

    @KafkaListener(topics = Topics.PAYMENT_SUCCESS, groupId = "notification-service")
    public void onSuccess(@Payload String payload,
                          @Header(name = TraceConstants.TRACE_HEADER, required = false) byte[] trace) {
        handle(trace, () -> {
            PaymentSuccessEvent e = read(payload, PaymentSuccessEvent.class);
            deliver(new NotificationMessage(e.paymentId(), "SUCCESS", "rail=" + e.rail() + " ref=" + e.settlementRef()));
        });
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "notification-service")
    public void onFailed(@Payload String payload,
                         @Header(name = TraceConstants.TRACE_HEADER, required = false) byte[] trace) {
        handle(trace, () -> {
            PaymentFailedEvent e = read(payload, PaymentFailedEvent.class);
            deliver(new NotificationMessage(e.paymentId(), "FAILED", e.errorReason()));
        });
    }

    private void deliver(NotificationMessage message) {
        dispatcher.dispatch(message);
        sns.publish(message);
    }

    private <T> T read(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Bad " + type.getSimpleName() + " payload", e);
        }
    }

    private void handle(byte[] trace, Runnable action) {
        if (trace != null) {
            MDC.put(TraceConstants.MDC_TRACE_ID, new String(trace, StandardCharsets.UTF_8));
        }
        try {
            action.run();
        } catch (Exception ex) {
            log.error("notification_failed", ex);
        } finally {
            MDC.remove(TraceConstants.MDC_TRACE_ID);
        }
    }
}
