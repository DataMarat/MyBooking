package com.mybooking.bookingservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * Конфигурация OpenAPI для Booking Service.
 *
 * <p>Определяет метаданные API и схему безопасности Bearer JWT,
 * используемую Swagger UI для выполнения авторизованных запросов к эндпоинтам сервиса.</p>
 */


@Configuration
public class OpenApiConfig {
    @Bean
    /**
     * Создаёт и настраивает объект {@link io.swagger.v3.oas.models.OpenAPI}.
     *
     * <p>Регистрирует HTTP-схему безопасности Bearer JWT, добавляет требование безопасности
     * для операций API и задаёт базовую информацию об API сервиса.</p>
     *
     * @return конфигурация OpenAPI для Swagger UI
     */
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .info(new Info().title("Booking Service API").version("v1"));
    }
}
