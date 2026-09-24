package com.ndz.payment_service.controller;

import com.ndz.payment_service.dto.CreatePaymentIntentRequest;
import com.ndz.payment_service.dto.PaymentIntentResponse;
import com.ndz.payment_service.dto.PaymentResponse;
import com.ndz.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/intent")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentIntentResponse createIntent(@Valid @RequestBody CreatePaymentIntentRequest request) {
        return paymentService.createIntent(request);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getById(@PathVariable UUID paymentId) {
        return paymentService.getById(paymentId);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String stripeSignature
    ) {
        paymentService.handleWebhook(payload, stripeSignature);
        return ResponseEntity.ok("ok");
    }
}
