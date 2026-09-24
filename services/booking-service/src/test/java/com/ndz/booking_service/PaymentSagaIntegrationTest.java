package com.ndz.booking_service;

import com.ndz.booking_service.client.VenueClient;
import com.ndz.booking_service.client.VenueSlotResponse;
import com.ndz.booking_service.entity.BookingStatus;
import com.ndz.booking_service.entity.Role;
import com.ndz.booking_service.messaging.PaymentEventHandler;
import com.ndz.booking_service.repository.BookingRepository;
import com.ndz.booking_service.repository.ProcessedEventRepository;
import com.ndz.booking_service.security.JwtService;
import com.ndz.booking_service.service.BookingService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PaymentSagaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES;

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

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
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @TestConfiguration
    static class Mocks {
        @Bean
        @Primary
        VenueClient venueClient() {
            return Mockito.mock(VenueClient.class);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtService jwtService;

    @Autowired
    VenueClient venueClient;

    @Autowired
    PaymentEventHandler paymentEventHandler;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    ProcessedEventRepository processedEventRepository;

    @Autowired
    BookingService bookingService;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll();
        doNothing().when(venueClient).invalidateShopSlotCache(any());
    }

    @Test
    void paymentSucceededConfirmsBookingAndIsIdempotent() throws Exception {
        UUID slotId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        when(venueClient.getSlot(slotId)).thenReturn(
                new VenueSlotResponse(slotId, shopId, UUID.randomUUID(), new BigDecimal("40"), "AVAILABLE")
        );

        String token = jwtService.generateAccessToken(UUID.randomUUID(), "saga@test.com", Role.USER, List.of());
        MvcResult createResult = mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("slotId", slotId))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID bookingId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asString()
        );
        UUID paymentId = UUID.randomUUID();

        String payload = objectMapper.writeValueAsString(Map.of(
                "eventType", PaymentEventHandler.PAYMENT_SUCCEEDED,
                "paymentId", paymentId.toString(),
                "bookingId", bookingId.toString(),
                "status", "SUCCEEDED",
                "amount", 40,
                "currency", "usd"
        ));

        paymentEventHandler.handle(payload);
        paymentEventHandler.handle(payload);

        mockMvc.perform(get("/bookings/" + bookingId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertThat(processedEventRepository.existsById(
                PaymentEventHandler.PAYMENT_SUCCEEDED + ":" + paymentId
        )).isTrue();
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    @Test
    void paymentFailedCancelsBooking() throws Exception {
        UUID slotId = UUID.randomUUID();
        when(venueClient.getSlot(slotId)).thenReturn(
                new VenueSlotResponse(slotId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("15"), "AVAILABLE")
        );

        String token = jwtService.generateAccessToken(UUID.randomUUID(), "fail@test.com", Role.USER, List.of());
        MvcResult createResult = mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("slotId", slotId))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID bookingId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asString()
        );

        paymentEventHandler.handle(objectMapper.writeValueAsString(Map.of(
                "eventType", PaymentEventHandler.PAYMENT_FAILED,
                "paymentId", UUID.randomUUID().toString(),
                "bookingId", bookingId.toString(),
                "status", "FAILED"
        )));

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void expireOverduePendingBookingsCancelsThem() throws Exception {
        UUID slotId = UUID.randomUUID();
        when(venueClient.getSlot(slotId)).thenReturn(
                new VenueSlotResponse(slotId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("12"), "AVAILABLE")
        );

        String token = jwtService.generateAccessToken(UUID.randomUUID(), "expire@test.com", Role.USER, List.of());
        MvcResult createResult = mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("slotId", slotId))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID bookingId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asString()
        );

        var booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        bookingRepository.save(booking);

        int expired = bookingService.expireOverduePendingBookings();
        assertThat(expired).isGreaterThanOrEqualTo(1);
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void confirmAndCancelHttpEndpointsAreRemoved() throws Exception {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "gone@test.com", Role.USER, List.of());
        UUID id = UUID.randomUUID();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/bookings/" + id + "/confirm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/bookings/" + id + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}