package com.mybooking.hotelservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Конфигурация безопасности Hotel Service.
 *
 * <p>Определяет правила авторизации HTTP-запросов и настраивает
 * обработку JWT-токенов в режиме OAuth2 Resource Server.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Конфигурация цепочки фильтров безопасности.
     *
     * <p>Разрешает доступ к техническим эндпоинтам без авторизации
     * и требует наличие валидного JWT для всех остальных запросов.</p>
     *
     * @param http объект конфигурации HTTP-безопасности
     * @return цепочка фильтров безопасности
     * @throws Exception при ошибке конфигурации
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF отключён, так как сервис работает в stateless-режиме с JWT
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Технические эндпоинты и консоль H2 доступны без авторизации
                        .requestMatchers("/actuator/**", "/h2-console/**").permitAll()
                        // Все остальные запросы требуют аутентификации
                        .anyRequest().authenticated()
                )
                // Настройка проверки JWT в качестве Bearer-токена
                .oauth2ResourceServer(oauth2 -> oauth2.jwt());

        // Разрешение отображения H2 Console во фрейме браузера
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    /**
     * Декодер JWT, использующий HMAC-ключ на основе общего секрета.
     *
     * @param secret секрет для подписи и проверки токенов
     * @return декодер JWT
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        return NimbusJwtDecoder
                .withSecretKey(JwtSecretKeyProvider.getHmacKey(secret))
                .build();
    }
}
