package com.sigmoid.paymentgateway.notification.sns;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigmoid.paymentgateway.notification.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Publishes the outcome to an SNS topic so downstream subscribers (push/SMS/webhook
 * services) receive it without this service changing. No-ops with a log line when
 * SNS is disabled or no topic ARN is configured.
 */
@Component
public class SnsPublisher {

    private static final Logger log = LoggerFactory.getLogger(SnsPublisher.class);

    private final ObjectProvider<SnsClient> snsProvider;
    private final NotificationProperties props;
    private final ObjectMapper objectMapper;

    public SnsPublisher(ObjectProvider<SnsClient> snsProvider, NotificationProperties props, ObjectMapper objectMapper) {
        this.snsProvider = snsProvider;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public void publish(NotificationMessage message) {
        SnsClient sns = snsProvider.getIfAvailable();
        if (sns == null || props.getTopicArn() == null || props.getTopicArn().isBlank()) {
            log.info("sns_skipped (disabled or no topicArn) paymentId={} outcome={}",
                    message.paymentId(), message.outcome());
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(message);
            sns.publish(PublishRequest.builder()
                    .topicArn(props.getTopicArn())
                    .subject("payment-" + message.outcome())
                    .message(body)
                    .build());
            log.info("sns_published paymentId={} topic={}", message.paymentId(), props.getTopicArn());
        } catch (Exception ex) {
            log.error("sns_publish_failed paymentId={}", message.paymentId(), ex);
        }
    }
}
