package com.sigmoid.paymentgateway.notification.channel;

import com.sigmoid.paymentgateway.notification.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PushChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(PushChannel.class);

    @Override
    public String name() {
        return "PUSH";
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("notify_push paymentId={} outcome={} detail={}", message.paymentId(), message.outcome(), message.detail());
    }
}
