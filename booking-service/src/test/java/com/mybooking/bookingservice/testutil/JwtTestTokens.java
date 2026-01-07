package com.mybooking.bookingservice.testutil;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Утилита генерации JWT для интеграционных тестов.
 *
 * <p>В production коде токены не генерируются booking-service. Здесь это нужно, чтобы
 * воспроизводимо тестировать защищённые эндпойнты без ручных действий.</p>
 */
public final class JwtTestTokens {

    private JwtTestTokens() {
    }

    /**
     * Генерирует HS256 JWT для указанного пользователя и scope (роль/права).
     *
     * @param secret HMAC секрет (>= 32 байта)
     * @param subject userId
     * @param scope scope, например "USER" или "ADMIN"
     * @return строка токена (без префикса "Bearer ")
     */
    public static String hmacToken(String secret, String subject, String scope) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes for HS256");
        }

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("scope", scope)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .build();

        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            JWSObject jws = new JWSObject(header, new Payload(claims.toJSONObject()));
            jws.sign(new MACSigner(key));
            return jws.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build test JWT", e);
        }
    }
}