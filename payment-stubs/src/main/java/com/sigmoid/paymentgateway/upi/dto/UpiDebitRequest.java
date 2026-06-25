package com.sigmoid.paymentgateway.upi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** POST /internal/upi/debit request body. */
public record UpiDebitRequest(

        @NotNull(message = "is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "is required")
        String vpa,

        @NotBlank(message = "is required")
        String refId
) {
}
