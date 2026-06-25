package com.sigmoid.paymentgateway.gateway.routing;

import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import com.sigmoid.paymentgateway.common.model.Rail;
import com.sigmoid.paymentgateway.gateway.api.dto.InitiatePaymentRequest;
import com.sigmoid.paymentgateway.gateway.config.RoutingProperties;
import com.sigmoid.paymentgateway.gateway.provider.ProviderClient;
import com.sigmoid.paymentgateway.gateway.provider.ProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Selects a provider for the requested rail and calls it. If the call fails or the
 * rail's breaker is OPEN, it walks the configured fallback order (e.g. UPI -> CARD)
 * until one succeeds. If every candidate fails, it raises PROVIDER_UNAVAILABLE.
 */
@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private final ProviderClient providerClient;
    private final RoutingProperties props;

    public RoutingService(ProviderClient providerClient, RoutingProperties props) {
        this.providerClient = providerClient;
        this.props = props;
    }

    public ProviderResult route(InitiatePaymentRequest req, String paymentId, String traceId) {
        List<Rail> candidates = props.candidatesFor(req.rail());
        for (Rail rail : candidates) {
            try {
                return providerClient.call(rail, req, paymentId, traceId);
            } catch (Exception ex) {
                log.warn("routing_attempt_failed rail={} paymentId={} reason={}", rail, paymentId, ex.toString());
            }
        }
        throw new ApiException(ErrorCode.PROVIDER_UNAVAILABLE,
                "All providers unavailable for rail " + req.rail() + " (tried " + candidates + ")");
    }
}
