package com.mybooking.bookingservice.api;

import java.time.Instant;

/**
 * Единый формат ошибки API.
 *
 * @param timestamp время формирования ответа
 * @param status HTTP статус
 * @param error краткое название ошибки (Reason Phrase)
 * @param message описание ошибки
 * @param path path запроса
 * @param traceId X-Request-Id из MDC (ключ traceId)
 */
public record ErrorDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId
) {}
