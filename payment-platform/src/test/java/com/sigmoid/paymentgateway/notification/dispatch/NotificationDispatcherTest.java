package com.sigmoid.paymentgateway.notification.dispatch;

import com.sigmoid.paymentgateway.notification.channel.NotificationChannel;
import com.sigmoid.paymentgateway.notification.model.NotificationMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationDispatcherTest {

    @Test
    void fansOutToEveryChannel() {
        NotificationChannel push = mock(NotificationChannel.class);
        NotificationChannel sms = mock(NotificationChannel.class);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(push, sms));

        NotificationMessage msg = new NotificationMessage("PAY-1", "SUCCESS", "rail=UPI");
        dispatcher.dispatch(msg);

        verify(push, times(1)).send(msg);
        verify(sms, times(1)).send(msg);
    }
}
