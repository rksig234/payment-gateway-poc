package com.sigmoid.paymentgateway.gateway.api;

import com.sigmoid.paymentgateway.common.error.ApiException;
import com.sigmoid.paymentgateway.common.error.ErrorCode;
import com.sigmoid.paymentgateway.common.trace.TraceContext;
import com.sigmoid.paymentgateway.gateway.api.dto.InitiatePaymentRequest;
import com.sigmoid.paymentgateway.gateway.api.dto.PaymentResponse;
import com.sigmoid.paymentgateway.gateway.service.PaymentService;
import com.sigmoid.paymentgateway.gateway.service.PaymentService.InitiateResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Write entry point for payments: POST /v1/payments. Reads (GET /v1/payments/{id}
 * and /timeline) are owned by the status package — the Postgres source of truth.
 */
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> initiate(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key header is required");
        }
        String traceId = TraceContext.currentTraceId();
        InitiateResult result = paymentService.initiate(request, idempotencyKey, traceId);

        HttpStatus statusCode = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(statusCode);
        if (result.replayed()) {
            builder.header("X-Idempotent-Replayed", "true");
        }
        return builder.body(result.response());
    }
}
