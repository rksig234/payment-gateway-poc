package com.sigmoid.paymentgateway.upi.dto;

/** 200 OK body: { "status": "SUCCESS", "utr": "UTR82910..." }. */
public record UpiDebitResponse(String status, String utr) {
}
