package com.sigmoid.paymentgateway.gateway.events;

import com.sigmoid.paymentgateway.common.event.PaymentFailedEvent;
import com.sigmoid.paymentgateway.common.event.PaymentInitiatedEvent;
import com.sigmoid.paymentgateway.common.event.PaymentRetryEvent;
import com.sigmoid.paymentgateway.common.event.PaymentSuccessEvent;
import com.sigmoid.paymentgateway.common.kafka.Topics;
import com.sigmoid.paymentgateway.common.trace.TraceConstants;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Publishes the four payment lifecycle events. The key is the paymentId (so all
 * events for a payment land on the same partition / stay ordered), and the traceId
 * rides in a Kafka header so the async hops stay on the same trace (Module D).
 */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public void initiated(PaymentInitiatedEvent e) {
        send(Topics.PAYMENT_INITIATED, e.paymentId(), e.traceId(), e);
    }

    public void success(PaymentSuccessEvent e) {
        send(Topics.PAYMENT_SUCCESS, e.paymentId(), e.traceId(), e);
    }

    public void failed(PaymentFailedEvent e) {
        send(Topics.PAYMENT_FAILED, e.paymentId(), e.traceId(), e);
    }

    public void retry(PaymentRetryEvent e) {
        send(Topics.PAYMENT_RETRY, e.paymentId(), e.traceId(), e);
    }

    private void send(String topic, String key, String traceId, Object payload) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);
        if (traceId != null) {
            record.headers().add(new RecordHeader(TraceConstants.TRACE_HEADER, traceId.getBytes(StandardCharsets.UTF_8)));
        }
        kafka.send(record);
        log.debug("published topic={} key={}", topic, key);
    }
}
