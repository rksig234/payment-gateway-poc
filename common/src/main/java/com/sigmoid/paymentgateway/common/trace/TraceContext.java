package com.sigmoid.paymentgateway.common.trace;

import org.slf4j.MDC;

/** Convenience accessor for the trace id bound to the current request thread. */
public final class TraceContext {

    private TraceContext() {
    }

    public static String currentTraceId() {
        return MDC.get(TraceConstants.MDC_TRACE_ID);
    }

    public static String currentRefId() {
        return MDC.get(TraceConstants.MDC_REF_ID);
    }
}
