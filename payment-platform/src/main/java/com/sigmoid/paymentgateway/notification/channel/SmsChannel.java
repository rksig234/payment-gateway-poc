package com.sigmoid.paymentgateway.notification.channel;

import com.sigmoid.paymentgateway.notification.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsChannel.class);

    @Override
    public String name() {
        return "SMS";
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("notify_sms paymentId={} outcome={}", message.paymentId(), message.outcome());
    }
}
