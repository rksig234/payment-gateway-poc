package com.sigmoid.paymentgateway.neft.dto;

import java.time.Instant;

/** 202 Accepted body: { "status": "PROCESSING", "ackEta": "T+2h", ... }. */
public record NeftTransferResponse(String status, String ackEta, Instant ackExpectedAt) {
}
