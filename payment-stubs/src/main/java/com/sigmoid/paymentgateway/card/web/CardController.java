package com.sigmoid.paymentgateway.card.web;

import com.sigmoid.paymentgateway.card.dto.CardChargeRequest;
import com.sigmoid.paymentgateway.card.dto.CardChargeResponse;
import com.sigmoid.paymentgateway.card.service.CardSimulator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Card rail entry point used by the Gateway Routing Service (Module A). */
@RestController
@RequestMapping("/internal/card")
public class CardController {

    private final CardSimulator simulator;

    public CardController(CardSimulator simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/charge")
    public ResponseEntity<CardChargeResponse> charge(@Valid @RequestBody CardChargeRequest request) {
        return ResponseEntity.ok(simulator.charge(request));
    }

    @PostMapping("/challenge/{challengeId}")
    public ResponseEntity<CardChargeResponse> completeChallenge(@PathVariable String challengeId) {
        return ResponseEntity.ok(simulator.completeChallenge(challengeId));
    }
}
