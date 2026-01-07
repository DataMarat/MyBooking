package com.mybooking.bookingservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke-тест для WireMock в booking-service.
 *
 * <p>Важно: webEnvironment=MOCK, чтобы был servlet-контекст и Spring Security создал HttpSecurity,
 * иначе SecurityConfig#filterChain(HttpSecurity) не соберётся.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class WireMockSmokeIntegrationTests {

    private static final WireMockServer WIREMOCK = new WireMockServer(wireMockConfig().dynamicPort());

    @BeforeAll
    static void startWireMock() {
        WIREMOCK.start();
        WireMock.configureFor("localhost", WIREMOCK.port());
    }

    @AfterAll
    static void stopWireMock() {
        WIREMOCK.stop();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("hotel.base-url", () -> "http://localhost:" + WIREMOCK.port());
        // на случай если эти пропсы обязательны в @ConfigurationProperties/@Value:
        r.add("hotel.timeout-ms", () -> "800");
        r.add("hotel.retries", () -> "1");
    }

    @Test
    void wireMockStartsOnRandomPortAndResponds() throws IOException {
        // Arrange
        WireMock.stubFor(WireMock.get("/__smoke").willReturn(WireMock.ok("ok")));

        // Act
        HttpURLConnection conn = (HttpURLConnection) URI.create("http://localhost:" + WIREMOCK.port() + "/__smoke")
                .toURL()
                .openConnection();
        conn.setRequestMethod("GET");

        // Assert
        assertThat(conn.getResponseCode()).isEqualTo(200);
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/__smoke")));
    }
}
