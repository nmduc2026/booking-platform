package com.ndz.booking_service;

import com.ndz.booking_service.client.VenueClient;
import com.ndz.booking_service.client.VenueSlotResponse;
import com.ndz.booking_service.entity.Role;
import com.ndz.booking_service.security.JwtService;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class BookingFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
        POSTGRES.withDatabaseName("booking_platform");
        POSTGRES.withUsername("booking");
        POSTGRES.withPassword("123123");
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    static class MockVenueConfig {
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

    @Test
    void createConfirmAndCancelBooking() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        when(venueClient.getSlot(slotId)).thenReturn(
                new VenueSlotResponse(slotId, shopId, UUID.randomUUID(), new BigDecimal("55.00"), "AVAILABLE")
        );

        String userToken = jwtService.generateAccessToken(userId, "user@test.com", Role.USER, List.of());

        MvcResult createResult = mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("slotId", slotId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(55.00))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookingId = UUID.fromString(created.get("id").asString());

        mockMvc.perform(get("/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(patch("/bookings/" + bookingId + "/confirm")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(patch("/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void rejectBookingWhenSlotNotAvailable() throws Exception {
        UUID slotId = UUID.randomUUID();
        when(venueClient.getSlot(any())).thenReturn(
                new VenueSlotResponse(slotId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10"), "BOOKED")
        );

        String userToken = jwtService.generateAccessToken(
                UUID.randomUUID(), "user2@test.com", Role.USER, List.of()
        );

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("slotId", slotId))))
                .andExpect(status().isConflict());
    }
}
