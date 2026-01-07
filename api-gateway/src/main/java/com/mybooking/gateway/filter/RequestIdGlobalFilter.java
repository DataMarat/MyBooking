package com.mybooking.gateway.filter;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Глобальный фильтр API Gateway для сквозного X-Request-Id.
 *
 * <p>Если клиент не прислал X-Request-Id, фильтр генерирует UUID и:
 * <ul>
 *   <li>прокидывает X-Request-Id во все downstream-сервисы,</li>
 *   <li>возвращает X-Request-Id клиенту в заголовках ответа.</li>
 * </ul>
 * </p>
 */
@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String X_REQUEST_ID = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(X_REQUEST_ID);

        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header(X_REQUEST_ID, requestId)
                .build();

        exchange.getResponse().getHeaders().set(X_REQUEST_ID, requestId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Чем меньше число — тем раньше фильтр выполняется.
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
