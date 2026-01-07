package com.mybooking.hotelservice.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Фильтр корреляции запросов: заголовок X-Request-Id + MDC.
 *
 * <p>Поведение:
 * <ul>
 *   <li>читает X-Request-Id (если отсутствует — генерирует UUID);</li>
 *   <li>кладёт значение в MDC под ключом traceId;</li>
 *   <li>добавляет X-Request-Id в response;</li>
 *   <li>очищает MDC в finally.</li>
 * </ul>
 * </p>
 */
@Component
public class RequestIdMdcFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_TRACE_ID, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
