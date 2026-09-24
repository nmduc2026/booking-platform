package com.ndz.payment_service;

import com.ndz.payment_service.entity.OutboxStatus;
import com.ndz.payment_service.entity.PaymentStatus;
import com.ndz.payment_service.entity.Role;
import com.ndz.payment_service.outbox.OutboxRelayService;
import com.ndz.payment_service.outbox.OutboxService;
import com.ndz.payment_service.outbox.PaymentEventPublisher;
import com.ndz.payment_service.repository.OutboxEventRepository;
import com.ndz.payment_service.repository.PaymentRepository;
import com.ndz.payment_service.security.JwtService;
import com.ndz.payment_service.stripe.StripeGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PaymentFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
        POSTGRES.withDatabaseName("booking_platform");
        POSTGRES.withUsername("booking");
        POSTGRES.withPassword("123123");
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    static class Mocks {
        @Bean
        @Primary
        StripeGateway stripeGateway() {
            return Mockito.mock(StripeGateway.class);
        }

        @Bean
        @Primary
        PaymentEventPublisher paymentEventPublisher() {
            return Mockito.mock(PaymentEventPublisher.class);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtService jwtService;

    @Autowired
    StripeGateway stripeGateway;

    @Autowired
    PaymentEventPublisher publisher;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    OutboxRelayService outboxRelayService;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();
        reset(stripeGateway, publisher);
        doNothing().when(publisher).publish(anyString(), anyString());
    }

    @Test
    void createIntentThenWebhookSucceededWritesOutboxAndRelayMarksSent() throws Exception {
        String stripePiId = "pi_test_" + UUID.randomUUID().toString().replace("-", "");
        when(stripeGateway.createPaymentIntent(anyLong(), anyString(), anyMap()))
                .thenReturn(new StripeGateway.CreatedPaymentIntent(stripePiId, stripePiId + "_secret", "requires_payment_method"));

        UUID bookingId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "pay@test.com", Role.USER, List.of());

        String body = mockMvc.perform(post("/payments/intent")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bookingId", bookingId,
                                "amount", new BigDecimal("55.00"),
                                "currency", "usd"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientSecret").value(stripePiId + "_secret"))
                .andExpect(jsonPath("$.status").value("REQUIRES_PAYMENT"))
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String paymentId = objectMapper.readTree(body).get("paymentId").asString();

        when(stripeGateway.parseWebhook(anyString(), anyString()))
                .thenReturn(new StripeGateway.WebhookEvent("payment_intent.succeeded", stripePiId));

        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=test")
                        .content("{\"id\":\"evt_test\"}"))
                .andExpect(status().isOk());

        assertThat(paymentRepository.findById(UUID.fromString(paymentId)).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(outboxEventRepository.countByStatus(OutboxStatus.PENDING)).isEqualTo(1);
        assertThat(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING).getFirst().getEventType())
                .isEqualTo(OutboxService.PAYMENT_SUCCEEDED);

        UUID eventId = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING).getFirst().getId();
        outboxRelayService.publishPendingEvent(eventId);

        assertThat(outboxEventRepository.findById(eventId).orElseThrow().getStatus()).isEqualTo(OutboxStatus.SENT);
        verify(publisher, times(1)).publish(eq(OutboxService.PAYMENT_SUCCEEDED), anyString());

        mockMvc.perform(get("/payments/" + paymentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void webhookPaymentFailedEnqueuesPaymentFailed() throws Exception {
        String stripePiId = "pi_fail_" + UUID.randomUUID().toString().replace("-", "");
        when(stripeGateway.createPaymentIntent(anyLong(), anyString(), anyMap()))
                .thenReturn(new StripeGateway.CreatedPaymentIntent(stripePiId, "secret", "requires_payment_method"));

        String token = jwtService.generateAccessToken(UUID.randomUUID(), "fail@test.com", Role.USER, List.of());
        mockMvc.perform(post("/payments/intent")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bookingId", UUID.randomUUID(),
                                "amount", 10
                        ))))
                .andExpect(status().isCreated());

        when(stripeGateway.parseWebhook(anyString(), anyString()))
                .thenReturn(new StripeGateway.WebhookEvent("payment_intent.payment_failed", stripePiId));

        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=test")
                        .content("{}"))
                .andExpect(status().isOk());

        assertThat(paymentRepository.findByStripePaymentIntentId(stripePiId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.FAILED);
        assertThat(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING).getFirst().getEventType())
                .isEqualTo(OutboxService.PAYMENT_FAILED);
    }

    @Test
    void webhookIsIdempotentForDuplicateSucceeded() throws Exception {
        String stripePiId = "pi_idem_" + UUID.randomUUID().toString().replace("-", "");
        when(stripeGateway.createPaymentIntent(anyLong(), anyString(), anyMap()))
                .thenReturn(new StripeGateway.CreatedPaymentIntent(stripePiId, "secret", "requires_payment_method"));
        when(stripeGateway.parseWebhook(anyString(), anyString()))
                .thenReturn(new StripeGateway.WebhookEvent("payment_intent.succeeded", stripePiId));

        String token = jwtService.generateAccessToken(UUID.randomUUID(), "idem@test.com", Role.USER, List.of());
        mockMvc.perform(post("/payments/intent")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bookingId", UUID.randomUUID(),
                                "amount", 20
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/payments/webhook")
                        .header("Stripe-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/payments/webhook")
                        .header("Stripe-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        assertThat(outboxEventRepository.countByStatus(OutboxStatus.PENDING)).isEqualTo(1);
    }
}
