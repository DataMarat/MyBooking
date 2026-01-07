package com.mybooking.hotelservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenAPI для сервиса управления отелями.
 *
 * <p>Определяет метаданные API и схему безопасности Bearer JWT,
 * используемую Swagger UI для выполнения авторизованных запросов.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Создаёт и настраивает объект {@link OpenAPI}.
     *
     * <p>Регистрирует HTTP-схему безопасности Bearer JWT,
     * добавляет требование безопасности для всех операций
     * и задаёт базовую информацию об API.</p>
     *
     * @return сконфигурированный экземпляр OpenAPI
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // Регистрация схемы безопасности Bearer JWT
                .components(new Components().addSecuritySchemes(
                        "bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                // Указание обязательного требования безопасности для операций API
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                // Метаданные API
                .info(new Info()
                        .title("Hotel Service API")
                        .version("1.0"));
    }
}
