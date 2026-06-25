package com.sigmoid.paymentgateway.notification.channel;

import com.sigmoid.paymentgateway.notification.model.NotificationMessage;

/**
 * A delivery channel (strategy pattern). POC implementations log; real providers
 * (FCM, an SMS gateway, a webhook caller) can be dropped in without other changes.
 */
public interface NotificationChannel {

    String name();

    void send(NotificationMessage message);
}
