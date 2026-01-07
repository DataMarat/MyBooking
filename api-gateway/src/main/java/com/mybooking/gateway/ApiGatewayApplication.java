package com.mybooking.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа API Gateway проекта MyBooking.
 *
 * <p>Шлюз маршрутизирует запросы к доменным сервисам (Hotel Service, Booking Service)
 * по префиксу <code>/api/**</code>, а также выступает как ресурсный сервер JWT
 * (проверка Bearer-токенов на входе).</p>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /**
     * Запускает Spring Boot приложение API Gateway.
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
