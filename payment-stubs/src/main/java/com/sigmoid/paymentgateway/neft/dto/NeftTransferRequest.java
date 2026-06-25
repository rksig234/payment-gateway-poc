package com.sigmoid.paymentgateway.neft.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/** POST /internal/neft/transfer request body. */
public record NeftTransferRequest(

        @NotNull(message = "is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "is required")
        @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "must be a valid IFSC code")
        String ifsc,

        @NotBlank(message = "is required")
        String refId
) {
}
