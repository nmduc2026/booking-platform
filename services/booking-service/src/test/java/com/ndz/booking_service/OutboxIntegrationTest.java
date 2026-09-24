package com.ndz.booking_service;

import com.ndz.booking_service.client.VenueClient;
import com.ndz.booking_service.client.VenueSlotResponse;
import com.ndz.booking_service.entity.OutboxStatus;
import com.ndz.booking_service.entity.Role;
import com.ndz.booking_service.outbox.BookingEventPublisher;
import com.ndz.booking_service.outbox.OutboxRelayService;
import com.ndz.booking_service.outbox.OutboxService;
import com.ndz.booking_service.repository.OutboxEventRepository;
import com.ndz.booking_service.security.JwtService;
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
import org.testcontainers.containers.GenericContainer;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OutboxIntegrationTest {

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

        @Bean
        @Primary
        BookingEventPublisher bookingEventPublisher() {
            return Mockito.mock(BookingEventPublisher.class);
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
    BookingEventPublisher publisher;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    OutboxRelayService outboxRelayService;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        reset(publisher);
        doNothing().when(venueClient).invalidateShopSlotCache(any());
        doNothing().when(publisher).publish(anyString(), anyString());
    }

    @Test
    void createBookingWritesPendingOutboxAndRelayMarksSent() throws Exception {
        UUID slotId = UUID.randomUUID();
        when(venueClient.getSlot(slotId)).thenReturn(
                new VenueSlotResponse(slotId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("40"), "AVAILABLE")
        );

        String token = jwtService.generateAccessToken(UUID.randomUUID(), "outbox@test.com", Role.USER, List.of());

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("slotId", slotId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(outboxEventRepository.countByStatus(OutboxStatus.PENDING)).isEqualTo(1);
        var pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING).getFirst();
        assertThat(pending.getEventType()).isEqualTo(OutboxService.BOOKING_CREATED);

        outboxRelayService.publishPendingEvent(pending.getId());

        assertThat(outboxEventRepository.findById(pending.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxStatus.SENT);
        assertThat(outboxEventRepository.countByStatus(OutboxStatus.PENDING)).isZero();
        verify(publisher, times(1)).publish(anyString(), anyString());
    }

    @Test
    void relayKeepsPendingWhenPublishFails() throws Exception {
        UUID slotId = UUID.randomUUID();
        when(venueClient.getSlot(slotId)).thenReturn(
                new VenueSlotResponse(slotId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("40"), "AVAILABLE")
        );
        doThrow(new RuntimeException("simulated crash")).when(publisher).publish(anyString(), anyString());

        String token = jwtService.generateAccessToken(UUID.randomUUID(), "crash@test.com", Role.USER, List.of());
        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("slotId", slotId))))
                .andExpect(status().isCreated());

        UUID eventId = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING).getFirst().getId();

        assertThatThrownBy(() -> outboxRelayService.publishPendingEvent(eventId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("simulated crash");

        assertThat(outboxEventRepository.findById(eventId)).isPresent();
        assertThat(outboxEventRepository.findById(eventId).orElseThrow().getStatus()).isEqualTo(OutboxStatus.PENDING);

        // Retry succeeds later
        reset(publisher);
        doNothing().when(publisher).publish(anyString(), anyString());
        outboxRelayService.publishPendingEvent(eventId);
        assertThat(outboxEventRepository.findById(eventId).orElseThrow().getStatus()).isEqualTo(OutboxStatus.SENT);
    }
}
