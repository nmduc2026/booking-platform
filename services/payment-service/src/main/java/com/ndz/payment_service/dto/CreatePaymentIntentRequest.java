package com.ndz.payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentIntentRequest(
        @NotNull UUID bookingId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String currency
) {
}
