package com.sigmoid.paymentgateway.gateway.routing;

import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.model.Rail;
import com.sigmoid.paymentgateway.gateway.api.dto.InitiatePaymentRequest;
import com.sigmoid.paymentgateway.gateway.config.RoutingProperties;
import com.sigmoid.paymentgateway.gateway.provider.ProviderClient;
import com.sigmoid.paymentgateway.gateway.provider.ProviderResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit tests for routing + fallback — no Spring context, Kafka, or Redis required. */
class RoutingServiceTest {

    private final ProviderClient providerClient = mock(ProviderClient.class);
    private final RoutingProperties props = new RoutingProperties(); // defaults: UPI -> [CARD]
    private final RoutingService routing = new RoutingService(providerClient, props);

    private final InitiatePaymentRequest upiReq =
            new InitiatePaymentRequest(BigDecimal.TEN, "INR", Rail.UPI, "merchant@bank", "CUST-1");

    @Test
    void usesRequestedRailWhenItSucceeds() {
        when(providerClient.call(eq(Rail.UPI), any(), any(), any()))
                .thenReturn(new ProviderResult(Rail.UPI, "UTR1", "SUCCESS"));

        ProviderResult result = routing.route(upiReq, "PAY-1", "trace-1");

        assertThat(result.provider()).isEqualTo(Rail.UPI);
    }

    @Test
    void fallsBackToNextRailWhenPrimaryFails() {
        when(providerClient.call(eq(Rail.UPI), any(), any(), any()))
                .thenThrow(new RuntimeException("UPI breaker open"));
        when(providerClient.call(eq(Rail.CARD), any(), any(), any()))
                .thenReturn(new ProviderResult(Rail.CARD, "UTR2", "SUCCESS"));

        ProviderResult result = routing.route(upiReq, "PAY-2", "trace-2");

        assertThat(result.provider()).isEqualTo(Rail.CARD);
    }

    @Test
    void raisesProviderUnavailableWhenAllCandidatesFail() {
        when(providerClient.call(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> routing.route(upiReq, "PAY-3", "trace-3"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("All providers unavailable");
    }
}
