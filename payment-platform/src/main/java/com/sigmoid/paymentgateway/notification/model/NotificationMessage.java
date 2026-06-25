package com.sigmoid.paymentgateway.notification.model;

/** The outcome to deliver to a customer across channels. */
public record NotificationMessage(String paymentId, String outcome, String detail) {
}
