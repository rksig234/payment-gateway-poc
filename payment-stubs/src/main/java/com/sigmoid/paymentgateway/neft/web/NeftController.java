package com.sigmoid.paymentgateway.neft.web;

import com.sigmoid.paymentgateway.neft.dto.NeftStatusResponse;
import com.sigmoid.paymentgateway.neft.dto.NeftTransferRequest;
import com.sigmoid.paymentgateway.neft.dto.NeftTransferResponse;
import com.sigmoid.paymentgateway.neft.service.NeftSimulator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** NEFT rail entry point used by the Gateway Routing Service (Module A). */
@RestController
@RequestMapping("/internal/neft")
public class NeftController {

    private final NeftSimulator simulator;

    public NeftController(NeftSimulator simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/transfer")
    public ResponseEntity<NeftTransferResponse> transfer(@Valid @RequestBody NeftTransferRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(simulator.accept(request));
    }

    @GetMapping("/transfer/{refId}")
    public ResponseEntity<NeftStatusResponse> status(@PathVariable String refId) {
        return ResponseEntity.ok(simulator.status(refId));
    }
}
