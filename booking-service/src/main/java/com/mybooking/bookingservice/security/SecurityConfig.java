package com.mybooking.bookingservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Конфигурация безопасности Booking Service.
 *
 * <p>Настраивает ресурсный сервер OAuth2 (JWT) и правила доступа:
 * публичными считаются только операции регистрации/логина, а также служебные эндпоинты.
 * Остальные запросы требуют валидного Bearer JWT.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityErrorHandler securityErrorHandler) throws Exception {
        http
                // CSRF нужно отключить/ослабить для H2 Console
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(PathRequest.toH2Console())
                )

                .authorizeHttpRequests(auth -> auth
                        // H2 console
                        .requestMatchers(PathRequest.toH2Console()).permitAll()

                        // публичные эндпойнты
                        .requestMatchers("/api/user/register", "/api/user/auth").permitAll()

                        // всё остальное — только с JWT
                        .anyRequest().authenticated()
                )

                // Единый JSON для 401/403 (не попадает в @RestControllerAdvice, т.к. это security layer)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                )

                // H2 Console рендерится во frame
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )

                // JWT Resource Server
                .oauth2ResourceServer(oauth2 -> oauth2.jwt());

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${security.jwt.secret:dev-secret-please-change}") String secret) {
        return NimbusJwtDecoder.withSecretKey(JwtSecretKeyProvider.getHmacKey(secret)).build();
    }
}
