package com.mybooking.bookingservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mybooking.bookingservice.model.Booking;
import com.mybooking.bookingservice.testutil.JwtTestTokens;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты саги booking-service с использованием WireMock вместо реального hotel-service.
 *
 * <p>Покрываем два сценария:
 * <ul>
 *   <li>Success: hold=200, confirm=200 → booking CONFIRMED</li>
 *   <li>Failure: hold=409/500 → booking CANCELLED + release best-effort</li>
 * </ul>
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingSagaWireMockIntegrationTests {

    private static final WireMockServer WIREMOCK = new WireMockServer(wireMockConfig().dynamicPort());

    /**
     * Секрет для HS256 JWT в тестах (>= 32 байта).
     */
    private static final String TEST_JWT_SECRET = "TEST_JWT_SECRET__MIN_32_BYTES_LONG__123456";

    @Autowired
    private TestRestTemplate rest;

    @BeforeAll
    static void startWireMock() {
        WIREMOCK.start();
        configureFor("localhost", WIREMOCK.port());
    }

    @AfterAll
    static void stopWireMock() {
        WIREMOCK.stop();
    }

    @BeforeEach
    void resetWireMock() {
        resetAllRequests();
        resetToDefault();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("hotel.base-url", () -> "http://localhost:" + WIREMOCK.port());
        r.add("hotel.timeout-ms", () -> "800");
        r.add("hotel.retries", () -> "1");
        r.add("security.jwt.secret", () -> TEST_JWT_SECRET);
    }

    @Test
    void sagaSuccess_shouldConfirmBooking_andPropagateRequestId() {
        // Arrange (Given)
        String requestId = UUID.randomUUID().toString();

        stubFor(post(urlPathEqualTo("/api/rooms/1/hold"))
                .willReturn(aResponse().withStatus(200)));

        stubFor(post(urlPathEqualTo("/api/rooms/confirm"))
                .willReturn(aResponse().withStatus(200)));

        // Act (When)
        ResponseEntity<Booking> resp = createBooking(requestId);

        // Assert (Then)
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(Booking.Status.CONFIRMED);
        assertThat(resp.getBody().getRequestId()).isEqualTo(requestId);

        verify(postRequestedFor(urlPathEqualTo("/api/rooms/1/hold"))
                .withHeader("X-Request-Id", equalTo(requestId)));

        verify(postRequestedFor(urlPathEqualTo("/api/rooms/confirm"))
                .withHeader("X-Request-Id", equalTo(requestId)));
    }

    @Test
    void sagaFailure_shouldCancelBooking_andReleaseBestEffort_andPropagateRequestId() {
        // Arrange (Given)
        String requestId = UUID.randomUUID().toString();

        stubFor(post(urlPathEqualTo("/api/rooms/1/hold"))
                .willReturn(aResponse().withStatus(409)));

        // best-effort release: можно 200 или 404 (в коде 404 не должен валить поток)
        stubFor(post(urlPathEqualTo("/api/rooms/release"))
                .willReturn(aResponse().withStatus(200)));

        // Act (When)
        ResponseEntity<Booking> resp = createBooking(requestId);

        // Assert (Then)
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(Booking.Status.CANCELLED);
        assertThat(resp.getBody().getRequestId()).isEqualTo(requestId);

        verify(postRequestedFor(urlPathEqualTo("/api/rooms/1/hold"))
                .withHeader("X-Request-Id", equalTo(requestId)));

        verify(postRequestedFor(urlPathEqualTo("/api/rooms/release"))
                .withHeader("X-Request-Id", equalTo(requestId)));

        // confirm не должен вызываться, если hold не прошёл
        verify(0, postRequestedFor(urlPathEqualTo("/api/rooms/confirm")));
    }

    private ResponseEntity<Booking> createBooking(String requestId) {
        String token = JwtTestTokens.hmacToken(TEST_JWT_SECRET, "1", "USER");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Request-Id", requestId);

        Map<String, String> body = Map.of(
                "roomId", "1",
                "startDate", LocalDate.now().plusDays(1).toString(),
                "endDate", LocalDate.now().plusDays(2).toString()
        );

        return rest.exchange(
                "/api/bookings",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Booking.class
        );
    }
}
