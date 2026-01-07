package com.mybooking.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Конфигурация JWT-декодера для API Gateway (WebFlux).
 *
 * <p>Gateway выступает ресурсным сервером и валидирует входящие Bearer JWT.
 * Для учебного проекта используется симметричный ключ HMAC (HS256).</p>
 */
@Configuration
public class JwtConfig {

    /**
     * Создаёт реактивный JWT-декодер для WebFlux Security.
     *
     * @param secret симметричный секрет для HS256 (минимум 32 байта; если меньше — дополняется)
     * @return ReactiveJwtDecoder
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${security.jwt.secret:development-secret-need-to-change}") String secret
    ) {
        return NimbusReactiveJwtDecoder.withSecretKey(JwtSecretKeyProvider.getHmacKey(secret)).build();
    }
}
