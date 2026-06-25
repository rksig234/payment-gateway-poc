package com.sigmoid.paymentgateway.common.obs;

/**
 * Metric names from the observability contract slide. Micrometer base names are
 * chosen so the Prometheus registry renders the exact contract names:
 *   payment.requests (counter) -> payment_requests_total{provider,status}
 *   payment.latency  (timer)   -> payment_latency_seconds{provider}
 */
public final class MetricNames {

    private MetricNames() {
    }

    public static final String REQUESTS = "payment.requests";
    public static final String LATENCY = "payment.latency";

    public static final String TAG_PROVIDER = "provider";
    public static final String TAG_STATUS = "status";
}
