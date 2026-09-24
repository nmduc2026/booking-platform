package com.ndz.payment_service.stripe;

import com.ndz.payment_service.config.StripeProperties;
import com.ndz.payment_service.exception.ApiException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StripeGatewayImpl implements StripeGateway {

    private final StripeProperties properties;

    public StripeGatewayImpl(StripeProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        Stripe.apiKey = properties.secretKey();
    }

    @Override
    public CreatedPaymentIntent createPaymentIntent(long amountCents, String currency, Map<String, String> metadata) {
        try {
            PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(currency)
                    .addPaymentMethodType("card");
            if (metadata != null) {
                metadata.forEach(builder::putMetadata);
            }
            PaymentIntent intent = PaymentIntent.create(builder.build());
            return new CreatedPaymentIntent(intent.getId(), intent.getClientSecret(), intent.getStatus());
        } catch (StripeException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to create Stripe PaymentIntent: " + ex.getMessage());
        }
    }

    @Override
    public WebhookEvent parseWebhook(String payload, String signatureHeader) {
        try {
            Event event = Webhook.constructEvent(payload, signatureHeader, properties.webhookSecret());
            String paymentIntentId = extractPaymentIntentId(event);
            return new WebhookEvent(event.getType(), paymentIntentId);
        } catch (SignatureVerificationException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature");
        }
    }

    private String extractPaymentIntentId(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);
        if (stripeObject instanceof PaymentIntent paymentIntent) {
            return paymentIntent.getId();
        }
        try {
            StripeObject unsafe = deserializer.deserializeUnsafe();
            if (unsafe instanceof PaymentIntent paymentIntent) {
                return paymentIntent.getId();
            }
        } catch (Exception ignored) {
            // fall through
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Webhook event does not contain a PaymentIntent");
    }
}
