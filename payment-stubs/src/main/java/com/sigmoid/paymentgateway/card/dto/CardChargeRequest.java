package com.sigmoid.paymentgateway.card.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** POST /internal/card/charge request body. panToken is a tokenized PAN (no raw card data). */
public record CardChargeRequest(

        @NotNull(message = "is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "is required")
        String panToken,

        @NotBlank(message = "is required")
        String refId
) {
}
