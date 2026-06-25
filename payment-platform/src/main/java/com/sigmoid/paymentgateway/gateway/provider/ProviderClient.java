package com.sigmoid.paymentgateway.gateway.provider;

import com.sigmoid.paymentgateway.common.model.Rail;
import com.sigmoid.paymentgateway.common.trace.TraceConstants;
import com.sigmoid.paymentgateway.gateway.api.dto.InitiatePaymentRequest;
import com.sigmoid.paymentgateway.gateway.config.RoutingProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls a single provider stub for a given rail, wrapped in that rail's
 * Resilience4j circuit breaker. A 4xx/5xx from the stub surfaces as an exception
 * (recorded by the breaker); an OPEN breaker short-circuits with
 * {@code CallNotPermittedException}. The caller (routing) handles both as "try the next rail".
 */
@Component
public class ProviderClient {

    private static final Logger log = LoggerFactory.getLogger(ProviderClient.class);

    private final RestClient stubsClient;
    private final RoutingProperties props;
    private final CircuitBreakerRegistry breakerRegistry;

    public ProviderClient(RestClient stubsClient, RoutingProperties props, CircuitBreakerRegistry breakerRegistry) {
        this.stubsClient = stubsClient;
        this.props = props;
        this.breakerRegistry = breakerRegistry;
    }

    public ProviderResult call(Rail rail, InitiatePaymentRequest req, String paymentId, String traceId) {
        CircuitBreaker breaker = breakerRegistry.circuitBreaker(rail.name().toLowerCase());
        return breaker.executeSupplier(() -> doCall(rail, req, paymentId, traceId));
    }

    private ProviderResult doCall(Rail rail, InitiatePaymentRequest req, String paymentId, String traceId) {
        Map<String, Object> body = buildBody(rail, req, paymentId);
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = stubsClient.post()
                .uri(props.pathFor(rail))
                .header(TraceConstants.TRACE_HEADER, traceId)
                .body(body)
                .retrieve()
                .body(Map.class);

        String status = resp == null ? null : String.valueOf(resp.get("status"));
        String settlementRef = settlementRefOf(resp);
        log.info("provider_call_ok rail={} paymentId={} status={} ref={}", rail, paymentId, status, settlementRef);
        return new ProviderResult(rail, settlementRef, status);
    }

    private Map<String, Object> buildBody(Rail rail, InitiatePaymentRequest req, String paymentId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", req.amount());
        body.put("refId", paymentId);
        switch (rail) {
            case UPI -> body.put("vpa", req.payeeVpa() != null ? req.payeeVpa() : "merchant@bank");
            case NEFT -> body.put("ifsc", props.getDefaultIfsc());
            case CARD -> body.put("panToken", props.getDefaultPanToken());
        }
        return body;
    }

    private String settlementRefOf(Map<String, Object> resp) {
        if (resp == null) {
            return null;
        }
        for (String k : new String[]{"utr", "ackEta", "challenge"}) {
            Object v = resp.get(k);
            if (v != null) {
                return String.valueOf(v);
            }
        }
        return null;
    }
}
