package com.sigmoid.paymentgateway.common.model;

/**
 * Payment rails supported by the gateway. One provider stub backs each rail.
 * Defined once here so routing (Module A) and status tracking (Module C) agree.
 */
public enum Rail {
    UPI,
    NEFT,
    CARD
}
