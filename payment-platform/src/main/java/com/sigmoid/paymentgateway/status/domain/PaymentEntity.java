package com.sigmoid.paymentgateway.status.domain;

import com.sigmoid.paymentgateway.common.model.PaymentStatus;
import com.sigmoid.paymentgateway.common.model.Rail;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** The durable payments row (deck data-layer schema). */
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @Column(name = "payment_id", length = 40)
    private String paymentId;

    @Column(name = "customer_id", length = 40)
    private String customerId;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "rail", length = 10)
    private Rail rail;

    @Column(name = "provider", length = 10)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 12)
    private PaymentStatus status;

    @Column(name = "retries")
    private int retries;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected PaymentEntity() {
    }

    public PaymentEntity(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Rail getRail() {
        return rail;
    }

    public void setRail(Rail rail) {
        this.rail = rail;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
