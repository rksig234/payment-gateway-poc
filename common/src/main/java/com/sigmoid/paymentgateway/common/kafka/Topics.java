package com.sigmoid.paymentgateway.common.kafka;

/** The four payment topics from the deck, plus the DLQ. One place so every service agrees. */
public final class Topics {

    private Topics() {
    }

    public static final String PAYMENT_INITIATED = "payment.initiated";
    public static final String PAYMENT_SUCCESS = "payment.success";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_RETRY = "payment.retry";

    /** Where the retry consumer parks messages after exhausting attempts. */
    public static final String PAYMENT_DLQ = "payment.dlq";
}
