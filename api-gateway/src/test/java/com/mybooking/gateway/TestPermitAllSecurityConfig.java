package com.mybooking.gateway;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Тестовая конфигурация Security для интеграционных тестов Gateway.
 * Полностью отключает требования Bearer/JWT, чтобы тестировать фильтры/маршрутизацию.
 */
@TestConfiguration
public class TestPermitAllSecurityConfig {

    @Bean
    public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .build();
    }
}
