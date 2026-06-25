package com.sigmoid.paymentgateway.notification.channel;

import com.sigmoid.paymentgateway.notification.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WebhookChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(WebhookChannel.class);

    @Override
    public String name() {
        return "WEBHOOK";
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("notify_webhook paymentId={} outcome={}", message.paymentId(), message.outcome());
    }
}
