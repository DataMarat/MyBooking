package com.mybooking.bookingservice.http;

import com.mybooking.bookingservice.logging.RequestIdMdcFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Interceptor для {@link org.springframework.web.client.RestClient}:
 * <ul>
 *   <li>прокидывает {@code X-Request-Id} из MDC в исходящие вызовы;</li>
 *   <li>прокидывает {@code Authorization: Bearer <token>} в service-to-service вызовы
 *       (используется входящий JWT пользователя).</li>
 * </ul>
 */
public class RequestIdRestClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        HttpHeaders headers = request.getHeaders();

        // 1) X-Request-Id
        if (!headers.containsKey(RequestIdMdcFilter.HEADER_REQUEST_ID)) {
            String traceId = MDC.get(RequestIdMdcFilter.MDC_TRACE_ID);
            if (StringUtils.hasText(traceId)) {
                headers.set(RequestIdMdcFilter.HEADER_REQUEST_ID, traceId);
            }
        }

        // 2) Authorization (Bearer)
        if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            String bearer = resolveBearerToken();
            if (StringUtils.hasText(bearer)) {
                headers.set(HttpHeaders.AUTHORIZATION, bearer);
            }
        }

        return execution.execute(request, body);
    }

    private String resolveBearerToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jat) {
            // getToken().getTokenValue() — исходный JWT как строка
            return "Bearer " + jat.getToken().getTokenValue();
        }
        return null;
    }
}
