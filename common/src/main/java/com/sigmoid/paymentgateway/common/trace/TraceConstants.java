package com.sigmoid.paymentgateway.common.trace;

/**
 * Header and MDC keys for trace propagation. The Gateway (Module A) generates a
 * traceId; every downstream HTTP hop forwards it. Module D later promotes this
 * to full OpenTelemetry, but the header contract is fixed here.
 */
public final class TraceConstants {

    private TraceConstants() {
    }

    /** Primary trace header used across all internal HTTP hops. */
    public static final String TRACE_HEADER = "X-Trace-Id";

    /** W3C trace context header; we extract the trace-id segment if present. */
    public static final String W3C_TRACEPARENT = "traceparent";

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_REF_ID = "refId";
}
