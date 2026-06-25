package com.sigmoid.paymentgateway.status.domain;

import com.sigmoid.paymentgateway.common.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One row per lifecycle transition — the complete history for a payment. */
@Entity
@Table(name = "payment_timeline")
public class PaymentTimelineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", length = 40)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 12)
    private PaymentStatus state;

    @Column(name = "event_time")
    private Instant eventTime;

    protected PaymentTimelineEntity() {
    }

    public PaymentTimelineEntity(String paymentId, PaymentStatus state, Instant eventTime) {
        this.paymentId = paymentId;
        this.state = state;
        this.eventTime = eventTime;
    }

    public Long getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getState() {
        return state;
    }

    public Instant getEventTime() {
        return eventTime;
    }
}
