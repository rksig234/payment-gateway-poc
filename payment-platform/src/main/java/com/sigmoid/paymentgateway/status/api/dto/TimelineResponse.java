package com.sigmoid.paymentgateway.status.api.dto;

import java.time.Instant;
import java.util.List;

/** GET /v1/payments/{id}/timeline body. */
public record TimelineResponse(String paymentId, String currentStatus, List<Transition> transitions) {

    public record Transition(String state, Instant ts) {
    }
}
