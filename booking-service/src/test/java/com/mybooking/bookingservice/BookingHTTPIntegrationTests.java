package com.mybooking.bookingservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-интеграционные тесты booking-service (MVC).
 *
 * <p>Проверяем:
 * <ul>
 *   <li>защищённость endpoint-а suggestions,</li>
 *   <li>успешный доступ при валидном JWT,</li>
 *   <li>корректный вызов hotel-service по /api/rooms (через WireMock).</li>
 * </ul>
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BookingHTTPIntegrationTests {

    private static final WireMockServer WIREMOCK = new WireMockServer(wireMockConfig().dynamicPort());

    /**
     * Секрет для HS256. Длина >= 32 байта обязательна.
     */
    private static final String TEST_JWT_SECRET = "TEST_JWT_SECRET__MIN_32_BYTES_LONG__123456";

    static {
        // Стартуем WireMock ДО вычисления DynamicPropertySource
        WIREMOCK.start();
        WireMock.configureFor("localhost", WIREMOCK.port());
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("hotel.base-url", () -> "http://localhost:" + WIREMOCK.port());
        r.add("hotel.timeout-ms", () -> "800");
        r.add("hotel.retries", () -> "1");

        r.add("security.jwt.secret", () -> TEST_JWT_SECRET);

        // отключаем Eureka в тестах
        r.add("eureka.client.enabled", () -> "false");
        r.add("eureka.client.register-with-eureka", () -> "false");
        r.add("eureka.client.fetch-registry", () -> "false");
    }

    @AfterAll
    static void stopWiremock() {
        WIREMOCK.stop();
    }

    @BeforeEach
    void reset() {
        WIREMOCK.resetAll();
    }

    @Test
    void suggestions_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/bookings/suggestions"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void suggestions_withJwt_success() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/rooms"))
                .willReturn(WireMock.okJson("""
                        [
                          {"id": 1, "number": "101", "timesBooked": 5},
                          {"id": 2, "number": "102", "timesBooked": 1}
                        ]
                        """)));

        mockMvc.perform(get("/api/bookings/suggestions")
                        .header("Authorization", "Bearer " + tokenUser()))
                .andExpect(status().isOk());
    }

    private String tokenUser() {
        try {
            byte[] secret = TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8);
            if (secret.length < 32) {
                throw new IllegalStateException("TEST_JWT_SECRET должен быть минимум 32 байта для HS256");
            }

            Instant now = Instant.now();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("test-user")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            JWSObject jws = new JWSObject(header, new Payload(claims.toJSONObject()));
            jws.sign(new MACSigner(secret));
            return jws.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сформировать тестовый JWT", e);
        }
    }
}
