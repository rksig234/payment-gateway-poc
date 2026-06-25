package com.sigmoid.paymentgateway.gateway.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import com.sigmoid.paymentgateway.gateway.api.dto.PaymentResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Idempotency via Redis (deck: SET key value EX 86400 NX). {@link #claim} atomically
 * reserves the key before any provider call; a second concurrent request with the same
 * key loses the race and reads the cached response instead of re-processing.
 */
@Service
public class IdempotencyService {

    private static final String IDEM_PREFIX = "idem:";
    private static final String PAYMENT_PREFIX = "payment:";
    private static final Duration TTL = Duration.ofSeconds(86_400); // 24h
    private static final String CLAIMED = "CLAIMED";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** Atomically claim the key. Returns false if it already exists (duplicate). */
    public boolean claim(String idempotencyKey) {
        Boolean ok = redis.opsForValue().setIfAbsent(IDEM_PREFIX + idempotencyKey, CLAIMED, TTL);
        return Boolean.TRUE.equals(ok);
    }

    /** Cached response for a key, or null if not present / still in-flight (placeholder only). */
    public PaymentResponse cachedResponse(String idempotencyKey) {
        String raw = redis.opsForValue().get(IDEM_PREFIX + idempotencyKey);
        if (raw == null || CLAIMED.equals(raw)) {
            return null;
        }
        return read(raw);
    }

    /** True if the key exists but only as the in-flight placeholder (claimed, not yet completed). */
    public boolean isInFlight(String idempotencyKey) {
        return CLAIMED.equals(redis.opsForValue().get(IDEM_PREFIX + idempotencyKey));
    }

    /** Persist the final response under both the idempotency key and the paymentId. */
    public void store(String idempotencyKey, String paymentId, PaymentResponse response) {
        String json = write(response);
        redis.opsForValue().set(IDEM_PREFIX + idempotencyKey, json, TTL);
        redis.opsForValue().set(PAYMENT_PREFIX + paymentId, json, TTL);
    }

    public PaymentResponse byPaymentId(String paymentId) {
        String raw = redis.opsForValue().get(PAYMENT_PREFIX + paymentId);
        if (raw == null) {
            throw new ApiException(ErrorCode.PAYMENT_NOT_FOUND, "No payment " + paymentId);
        }
        return read(raw);
    }

    private String write(PaymentResponse r) {
        try {
            return objectMapper.writeValueAsString(r);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to serialize response");
        }
    }

    private PaymentResponse read(String raw) {
        try {
            return objectMapper.readValue(raw, PaymentResponse.class);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to read cached response");
        }
    }
}
