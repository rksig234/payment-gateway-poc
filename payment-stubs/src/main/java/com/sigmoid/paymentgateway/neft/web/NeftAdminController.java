package com.sigmoid.paymentgateway.neft.web;

import com.sigmoid.paymentgateway.neft.config.NeftStubConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime config knobs for the NEFT stub. */
@RestController
@RequestMapping("/internal/admin/neft")
public class NeftAdminController {

    private final NeftStubConfig config;

    public NeftAdminController(NeftStubConfig config) {
        this.config = config;
    }

    @GetMapping("/config")
    public Map<String, Object> current() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("provider", "NEFT");
        view.put("ackDelayMin", config.getAckDelayMin());
        view.put("batchWindow", config.getBatchWindow());
        view.put("ackDelaySeconds", config.getAckDelaySeconds());
        return view;
    }

    @PostMapping("/config")
    public Map<String, Object> update(@RequestBody UpdateRequest body) {
        config.apply(body.ackDelayMin(), body.batchWindow(), body.ackDelaySeconds());
        return current();
    }

    /** Partial update — only non-null fields are applied. */
    public record UpdateRequest(Integer ackDelayMin, Integer batchWindow, Integer ackDelaySeconds) {
    }
}
