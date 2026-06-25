package com.sigmoid.paymentgateway.retry.events;

import com.sigmoid.paymentgateway.common.event.PaymentRetryEvent;
import com.sigmoid.paymentgateway.common.kafka.Topics;
import com.sigmoid.paymentgateway.common.trace.TraceConstants;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Re-publishes payment.retry (for the next attempt) and payment.dlq (on escalation). */
@Component
public class RetryEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public RetryEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public void publishRetry(PaymentRetryEvent e) {
        send(Topics.PAYMENT_RETRY, e.paymentId(), e.traceId(), e);
    }

    public void publishToDlq(String paymentId, String traceId, Object payload) {
        send(Topics.PAYMENT_DLQ, paymentId, traceId, payload);
    }

    private void send(String topic, String key, String traceId, Object payload) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);
        if (traceId != null) {
            record.headers().add(new RecordHeader(TraceConstants.TRACE_HEADER, traceId.getBytes(StandardCharsets.UTF_8)));
        }
        kafka.send(record);
    }
}
