package com.mybooking.hotelservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Точка входа в микросервис управления отелями (Hotel Service).
 *
 * <p>Данное Spring Boot приложение представляет микросервис,
 * отвечающий за управление отелями, номерами и связанными с ними данными.</p>
 *
 * <p>Аннотация {@link EnableDiscoveryClient} включает регистрацию сервиса
 * в системе обнаружения сервисов (Eureka), что позволяет другим микросервисам
 * (например, API Gateway или Booking Service) находить данный сервис
 * динамически по имени, а не по жёстко заданному адресу.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class HotelServiceApplication {

    /**
     * Точка запуска Spring Boot приложения.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        // Инициализация Spring-контекста и запуск встроенного веб-сервера
        SpringApplication.run(HotelServiceApplication.class, args);
    }
}
