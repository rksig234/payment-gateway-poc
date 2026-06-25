package com.sigmoid.paymentgateway.upi.web;

import com.sigmoid.paymentgateway.upi.dto.UpiDebitRequest;
import com.sigmoid.paymentgateway.upi.dto.UpiDebitResponse;
import com.sigmoid.paymentgateway.upi.service.UpiSimulator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UPI rail entry point used by the Gateway Routing Service (Module A). */
@RestController
@RequestMapping("/internal/upi")
public class UpiController {

    private final UpiSimulator simulator;

    public UpiController(UpiSimulator simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/debit")
    public ResponseEntity<UpiDebitResponse> debit(@Valid @RequestBody UpiDebitRequest request) {
        return ResponseEntity.ok(simulator.debit(request));
    }
}
