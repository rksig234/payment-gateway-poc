package com.sigmoid.paymentgateway.common.model;

/**
 * Payment state machine shared across services:
 * INITIATED -> PROCESSING -> SUCCESS | FAILED
 * (Payment Status Service in Module C owns transitions; stubs only emit terminal-ish hints.)
 */
public enum PaymentStatus {
    INITIATED,
    PROCESSING,
    SUCCESS,
    FAILED
}
