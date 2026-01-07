package com.mybooking.gateway;

import com.mybooking.gateway.filter.RequestIdGlobalFilter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = RequestIdGlobalFilterIntegrationTests.RouteInitializer.class)
@Import(TestPermitAllSecurityConfig.class)
class RequestIdGlobalFilterIntegrationTests {

    @Autowired
    WebTestClient webTestClient;

    static DisposableServer downstream;

    @BeforeAll
    static void startDownstream() {
        downstream = HttpServer.create()
                .port(0) // random free port
                .route(routes -> routes.get("/downstream/ping", (req, res) -> res.sendString(reactor.core.publisher.Mono.just("OK"))))
                .bindNow();
    }

    @AfterAll
    static void stopDownstream() {
        if (downstream != null) downstream.disposeNow();
    }

    @Test
    void whenNoRequestId_gatewayAddsHeader() {
        webTestClient.get()
                .uri("/test/ping")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(RequestIdGlobalFilter.X_REQUEST_ID);
    }

    @Test
    void whenRequestIdProvided_gatewayEchoesSameHeader() {
        String rid = "test-123";

        WebTestClient.ResponseSpec resp = webTestClient.get()
                .uri("/test/ping")
                .header(RequestIdGlobalFilter.X_REQUEST_ID, rid)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(RequestIdGlobalFilter.X_REQUEST_ID, v -> assertThat(v).isEqualTo(rid));
    }

    /**
     * Подменяем маршруты gateway на тестовый route, чтобы не зависеть от Eureka/других сервисов.
     */
    static class RouteInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            int port = downstream.port();

            TestPropertyValues.of(
                    "spring.cloud.gateway.routes[0].id=test-route",
                    "spring.cloud.gateway.routes[0].uri=http://localhost:" + port,
                    "spring.cloud.gateway.routes[0].predicates[0]=Path=/test/**",
                    "spring.cloud.gateway.routes[0].filters[0]=RewritePath=/test/(?<segment>.*),/downstream/${segment}"
            ).applyTo(context.getEnvironment());
        }
    }
}
