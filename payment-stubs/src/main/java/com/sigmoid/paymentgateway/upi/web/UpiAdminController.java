package com.sigmoid.paymentgateway.upi.web;

import com.sigmoid.paymentgateway.upi.config.UpiStubConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime config knobs for the UPI stub. Lets a demo flip latency / failure rate
 * on the fly to drive the Resilience4j circuit breaker (Module A) OPEN and back.
 */
@RestController
@RequestMapping("/internal/admin/upi")
public class UpiAdminController {

    private final UpiStubConfig config;

    public UpiAdminController(UpiStubConfig config) {
        this.config = config;
    }

    @GetMapping("/config")
    public Map<String, Object> current() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("provider", "UPI");
        view.put("latencyMs", config.getLatencyMs());
        view.put("failureRate", config.getFailureRate());
        return view;
    }

    @PostMapping("/config")
    public Map<String, Object> update(@RequestBody UpdateRequest body) {
        config.apply(body.latencyMs(), body.failureRate());
        return current();
    }

    /** Partial update — only non-null fields are applied. */
    public record UpdateRequest(Integer latencyMs, Integer failureRate) {
    }
}
