package com.sigmoid.paymentgateway.gateway.provider;

import com.sigmoid.paymentgateway.common.model.Rail;

/** Outcome of a successful provider call: which rail handled it and its settlement reference. */
public record ProviderResult(Rail provider, String settlementRef, String providerStatus) {
}
