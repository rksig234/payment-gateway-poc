package com.sigmoid.paymentgateway.card.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 200 OK body. Frictionless: { "status": "SUCCESS", "challenge": "3DS_PASSED" }.
 * Challenge required: { "status": "REQUIRES_CHALLENGE", "challenge": "3DS_REQUIRED", "challengeId": "3DS-..." }.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardChargeResponse(String status, String challenge, String challengeId, String refId) {
}
