package com.mybooking.bookingservice.api;

import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * Ответ на успешную регистрацию/аутентификацию.
 *
 * @param accessToken JWT токен доступа
 * @param tokenType тип токена (Bearer)
 */
public record AuthTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType
) {
    public static AuthTokenResponse bearer(String token) {
        return new AuthTokenResponse(token, "Bearer");
    }
}