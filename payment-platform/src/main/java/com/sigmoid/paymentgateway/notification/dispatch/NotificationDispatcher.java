package com.sigmoid.paymentgateway.notification.dispatch;

import com.sigmoid.paymentgateway.notification.channel.NotificationChannel;
import com.sigmoid.paymentgateway.notification.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** Fans a message out across every registered channel; one channel failing doesn't block the rest. */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final List<NotificationChannel> channels;

    public NotificationDispatcher(List<NotificationChannel> channels) {
        this.channels = channels;
    }

    public void dispatch(NotificationMessage message) {
        for (NotificationChannel channel : channels) {
            try {
                channel.send(message);
            } catch (Exception ex) {
                log.error("channel_failed channel={} paymentId={}", channel.name(), message.paymentId(), ex);
            }
        }
    }
}
