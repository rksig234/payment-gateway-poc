package com.sigmoid.paymentgateway.retry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A payment that exhausted its retries (deck dlq_records table). */
@Entity
@Table(name = "dlq_records")
public class DlqRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", length = 40)
    private String paymentId;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "error")
    private String error;

    @Column(name = "attempt_count")
    private int attemptCount;

    @Column(name = "moved_at")
    private Instant movedAt;

    protected DlqRecordEntity() {
    }

    public DlqRecordEntity(String paymentId, String payload, String error, int attemptCount, Instant movedAt) {
        this.paymentId = paymentId;
        this.payload = payload;
        this.error = error;
        this.attemptCount = attemptCount;
        this.movedAt = movedAt;
    }

    public Long getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getPayload() {
        return payload;
    }

    public String getError() {
        return error;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getMovedAt() {
        return movedAt;
    }
}
