package com.mybooking.hotelservice;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные HTTP-тесты Hotel Service через MockMvc.
 *
 * <p>Проверяют базовые сценарии авторизации по JWT и доступ к административным
 * эндпоинтам без поднятия реального сервера. Тесты используют MockMvc и заголовок
 * {@code Authorization: Bearer <token>}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
public class HotelHTTPIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Генерирует тестовый JWT для роли ADMIN.
     *
     * <p>Секрет соответствует значению {@code security.jwt.secret} в конфигурации сервиса.
     * Токен подписывается HMAC и используется в заголовке Authorization.</p>
     */
    private String tokenAdmin() {
        byte[] bytes = "development-secret-need-to-change".getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject("1")
                .addClaims(Map.of("scope", "ADMIN", "username", "admin"))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(bytes))
                .compact();
    }

    @Test
    void adminCanCreateHotel() throws Exception {
        mockMvc.perform(post("/api/hotels")
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"H\",\"city\":\"C\",\"address\":\"A\"}"))
                .andExpect(status().isOk());
    }
}
