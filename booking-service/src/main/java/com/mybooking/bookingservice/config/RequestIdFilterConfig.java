package com.mybooking.bookingservice.config;

import com.mybooking.bookingservice.logging.RequestIdMdcFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Регистрация RequestIdMdcFilter с приоритетом выше Spring Security,
 * чтобы X-Request-Id/MDC устанавливались даже при 401/403.
 */
@Configuration
public class RequestIdFilterConfig {

    @Bean
    public FilterRegistrationBean<RequestIdMdcFilter> requestIdMdcFilterRegistration(RequestIdMdcFilter filter) {
        FilterRegistrationBean<RequestIdMdcFilter> registration = new FilterRegistrationBean<>(filter);

        // Должно быть раньше springSecurityFilterChain (у него отрицательный order).
        // HIGHEST_PRECEDENCE гарантирует самый ранний запуск.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }
}
