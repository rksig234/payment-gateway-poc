package com.sigmoid.paymentgateway.gateway.api.dto;

import com.sigmoid.paymentgateway.common.model.Rail;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** POST /v1/payments request body (deck: amount, currency, rail, payeeVpa, customerId). */
public record InitiatePaymentRequest(

        @NotNull(message = "is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "is required")
        String currency,

        @NotNull(message = "is required")
        Rail rail,

        String payeeVpa,

        @NotBlank(message = "is required")
        String customerId
) {
}
