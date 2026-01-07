package com.mybooking.bookingservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybooking.bookingservice.api.ErrorDto;
import com.mybooking.bookingservice.logging.RequestIdMdcFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Единый JSON-ответ для ошибок безопасности:
 * - 401 Unauthorized (AuthenticationEntryPoint)
 * - 403 Forbidden (AccessDeniedHandler)
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException authException
    ) throws IOException, ServletException {
        write(response, request, HttpStatus.UNAUTHORIZED, "Unauthorized", "Unauthorized");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        write(response, request, HttpStatus.FORBIDDEN, "Forbidden", "Forbidden");
    }

    private void write(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String error,
            String message
    ) throws IOException {

        String traceId = MDC.get(RequestIdMdcFilter.MDC_TRACE_ID);

        ErrorDto body = new ErrorDto(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI(),
                traceId
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
