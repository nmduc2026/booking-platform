package com.ndz.payment_service.dto;

import com.ndz.payment_service.entity.Payment;
import com.ndz.payment_service.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentIntentResponse(
        UUID paymentId,
        UUID bookingId,
        String stripePaymentIntentId,
        String clientSecret,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt
) {
    public static PaymentIntentResponse from(Payment payment, String clientSecret) {
        return new PaymentIntentResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getStripePaymentIntentId(),
                clientSecret,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}
