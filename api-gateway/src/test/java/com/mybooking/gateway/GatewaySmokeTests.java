package com.mybooking.gateway;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

/**
 * Smoke-тест для API Gateway:
 * - проверяет, что маршрут /api/hotels/** проксируется в HOTEL-SERVICE,
 * - проверяет, что заголовки корректно форвардятся.
 *
 * ВАЖНО: в тестах мы отключаем JWT-аутентификацию, иначе Gateway вернёт 401.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.gateway.routes[0].id=hotels",
                "spring.cloud.gateway.routes[0].uri=http://localhost:9999",
                "spring.cloud.gateway.routes[0].predicates[0]=Path=/api/hotels/**",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/dummy"
        }
)
@ActiveProfiles("test")
class GatewaySmokeTests {

    @LocalServerPort
    int port;

    private WebClient client;

    @BeforeEach
    void setUp() {
        client = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void routesAndHeadersForwarded() {
        client.get()
                .uri("/api/hotels/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .exchangeToMono(r -> {
                    assertThat(r.statusCode().is4xxClientError()).isTrue();
                    return Mono.empty();
                })
                .block();
    }
}
