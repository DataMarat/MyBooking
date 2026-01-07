package com.mybooking.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Конфигурация безопасности API Gateway.
 *
 * <p>Шлюз действует как ресурсный сервер OAuth2 JWT и обеспечивает:
 * <ul>
 *   <li>публичный доступ к <code>/api/auth/**</code> (логин/регистрация),</li>
 *   <li>публичный доступ к <code>/actuator/**</code>,</li>
 *   <li>JWT-аутентификацию для всех остальных запросов.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Создаёт цепочку фильтров безопасности для Spring Cloud Gateway (WebFlux).
     */
    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        // Публичные маршруты авторизации/регистрации (через Gateway)
                        .pathMatchers("/api/auth/**", "/api/user/**").permitAll()

                        // Технические/документационные маршруты
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Всё остальное требует JWT
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(ServerHttpSecurity.OAuth2ResourceServerSpec::jwt);

        return http.build();
    }
}
