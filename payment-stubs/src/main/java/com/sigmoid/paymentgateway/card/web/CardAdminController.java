package com.sigmoid.paymentgateway.card.web;

import com.sigmoid.paymentgateway.card.config.CardStubConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime config knobs for the Card stub. */
@RestController
@RequestMapping("/internal/admin/card")
public class CardAdminController {

    private final CardStubConfig config;

    public CardAdminController(CardStubConfig config) {
        this.config = config;
    }

    @GetMapping("/config")
    public Map<String, Object> current() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("provider", "CARD");
        view.put("timeoutRate", config.getTimeoutRate());
        view.put("challengeRate", config.getChallengeRate());
        view.put("timeoutMs", config.getTimeoutMs());
        return view;
    }

    @PostMapping("/config")
    public Map<String, Object> update(@RequestBody UpdateRequest body) {
        config.apply(body.timeoutRate(), body.challengeRate(), body.timeoutMs());
        return current();
    }

    /** Partial update — only non-null fields are applied. */
    public record UpdateRequest(Integer timeoutRate, Integer challengeRate, Integer timeoutMs) {
    }
}
