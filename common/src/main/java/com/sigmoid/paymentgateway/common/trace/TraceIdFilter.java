package com.sigmoid.paymentgateway.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Binds a traceId to every request: reuses an inbound {@code X-Trace-Id} (or the
 * trace-id segment of a W3C {@code traceparent}), otherwise mints a new one.
 * The id is put on the MDC (so it appears in every log line) and echoed back on
 * the response so callers and Jaeger (Module D) can stitch the full journey.
 */
@Component
@Order(Integer.MIN_VALUE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(TraceConstants.MDC_TRACE_ID, traceId);
        response.setHeader(TraceConstants.TRACE_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TraceConstants.MDC_TRACE_ID);
            MDC.remove(TraceConstants.MDC_REF_ID);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TraceConstants.TRACE_HEADER);
        if (header != null && !header.isBlank()) {
            return header;
        }
        String traceparent = request.getHeader(TraceConstants.W3C_TRACEPARENT);
        if (traceparent != null && !traceparent.isBlank()) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 2 && !parts[1].isBlank()) {
                return parts[1];
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
