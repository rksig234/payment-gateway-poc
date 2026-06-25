package com.sigmoid.paymentgateway.gateway.config;

import com.sigmoid.paymentgateway.common.model.Rail;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Routing configuration: where the provider stubs live, the per-rail endpoint path,
 * and the fallback order consulted when a provider's breaker is OPEN.
 */
@ConfigurationProperties(prefix = "gateway.routing")
public class RoutingProperties {

    /** Base URL of the stubs app (all three rails are paths under it). */
    private String stubsBaseUrl = "http://localhost:8080";

    /** POC shim: NEFT stub requires a valid IFSC; the gateway request carries none. */
    private String defaultIfsc = "HDFC0001234";

    /** POC shim: Card stub requires a tokenized PAN; the gateway request carries none. */
    private String defaultPanToken = "tok_gateway";

    /** Per-rail request path on the stubs app. */
    private Map<Rail, String> paths = Map.of(
            Rail.UPI, "/internal/upi/debit",
            Rail.NEFT, "/internal/neft/transfer",
            Rail.CARD, "/internal/card/charge"
    );

    /** Fallback order per requested rail (e.g. UPI -> [CARD]). */
    private Map<Rail, List<Rail>> fallback = Map.of(
            Rail.UPI, List.of(Rail.CARD),
            Rail.CARD, List.of(Rail.UPI),
            Rail.NEFT, List.of()
    );

    public String getStubsBaseUrl() {
        return stubsBaseUrl;
    }

    public void setStubsBaseUrl(String stubsBaseUrl) {
        this.stubsBaseUrl = stubsBaseUrl;
    }

    public String getDefaultIfsc() {
        return defaultIfsc;
    }

    public void setDefaultIfsc(String defaultIfsc) {
        this.defaultIfsc = defaultIfsc;
    }

    public String getDefaultPanToken() {
        return defaultPanToken;
    }

    public void setDefaultPanToken(String defaultPanToken) {
        this.defaultPanToken = defaultPanToken;
    }

    public Map<Rail, String> getPaths() {
        return paths;
    }

    public void setPaths(Map<Rail, String> paths) {
        this.paths = paths;
    }

    public Map<Rail, List<Rail>> getFallback() {
        return fallback;
    }

    public void setFallback(Map<Rail, List<Rail>> fallback) {
        this.fallback = fallback;
    }

    /** Ordered candidates to try for a requested rail: the rail itself, then its fallbacks. */
    public List<Rail> candidatesFor(Rail rail) {
        List<Rail> fb = fallback.getOrDefault(rail, List.of());
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(rail), fb.stream()).toList();
    }

    public String pathFor(Rail rail) {
        return paths.get(rail);
    }
}
