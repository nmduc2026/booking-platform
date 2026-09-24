package com.ndz.payment_service.service;

import com.ndz.payment_service.dto.CreatePaymentIntentRequest;
import com.ndz.payment_service.dto.PaymentIntentResponse;
import com.ndz.payment_service.dto.PaymentResponse;
import com.ndz.payment_service.entity.Payment;
import com.ndz.payment_service.entity.PaymentStatus;
import com.ndz.payment_service.exception.ApiException;
import com.ndz.payment_service.outbox.OutboxService;
import com.ndz.payment_service.repository.PaymentRepository;
import com.ndz.payment_service.stripe.StripeGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final StripeGateway stripeGateway;
    private final OutboxService outboxService;

    public PaymentService(
            PaymentRepository paymentRepository,
            StripeGateway stripeGateway,
            OutboxService outboxService
    ) {
        this.paymentRepository = paymentRepository;
        this.stripeGateway = stripeGateway;
        this.outboxService = outboxService;
    }

    @Transactional
    public PaymentIntentResponse createIntent(CreatePaymentIntentRequest request) {
        String currency = (request.currency() == null || request.currency().isBlank())
                ? "usd"
                : request.currency().trim().toLowerCase();

        UUID paymentId = UUID.randomUUID();
        long amountCents = toCents(request.amount());

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("paymentId", paymentId.toString());
        metadata.put("bookingId", request.bookingId().toString());

        StripeGateway.CreatedPaymentIntent intent =
                stripeGateway.createPaymentIntent(amountCents, currency, metadata);

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setBookingId(request.bookingId());
        payment.setStripePaymentIntentId(intent.id());
        payment.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.REQUIRES_PAYMENT);
        paymentRepository.save(payment);

        return PaymentIntentResponse.from(payment, intent.clientSecret());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        return PaymentResponse.from(payment);
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        StripeGateway.WebhookEvent event = stripeGateway.parseWebhook(payload, signatureHeader);

        if ("payment_intent.succeeded".equals(event.type())) {
            markTerminal(event.paymentIntentId(), PaymentStatus.SUCCEEDED, OutboxService.PAYMENT_SUCCEEDED);
            return;
        }
        if ("payment_intent.payment_failed".equals(event.type())) {
            markTerminal(event.paymentIntentId(), PaymentStatus.FAILED, OutboxService.PAYMENT_FAILED);
            return;
        }

        log.debug("Ignoring Stripe event type {}", event.type());
    }

    private void markTerminal(String stripePaymentIntentId, PaymentStatus targetStatus, String outboxEventType) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found for PaymentIntent " + stripePaymentIntentId
                ));

        if (payment.getStatus() == targetStatus) {
            log.info("Idempotent webhook: payment {} already {}", payment.getId(), targetStatus);
            return;
        }
        if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Payment " + payment.getId() + " is already " + payment.getStatus());
        }

        payment.setStatus(targetStatus);
        outboxService.enqueue(outboxEventType, payment);
        log.info("Payment {} marked {} (booking {})", payment.getId(), targetStatus, payment.getBookingId());
    }

    private static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
