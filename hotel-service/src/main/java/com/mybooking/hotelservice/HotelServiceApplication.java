package com.mybooking.hotelservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Точка входа Spring Boot-приложения hotel-service.
 * Здесь только запуск и предзаполнение H2 начальными данными.
 */
@SpringBootApplication
public class HotelServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelServiceApplication.class, args);
    }

    /**
     * Предзаполнение базы данных 5 отелями при старте.
     * CommandLineRunner выполняется один раз после инициализации Spring-контекста.
     */
    @Bean
    CommandLineRunner initData(HotelRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.saveAll(List.of(
                        new Hotel(null, "Cosmos Nevsky", "Russia, St. Petersburg, Nevsky Ave 171"),
                        new Hotel(null, "Ibis Kazan Center", "Russia, Kazan, Pravo-Bulachnaya 43/1"),
                        new Hotel(null, "Azimut Moscow Olympic", "Russia, Moscow, Olimpiyskiy Ave 18/1"),
                        new Hotel(null, "Park Inn Sochi City", "Russia, Sochi, Gorkogo 56"),
                        new Hotel(null, "Novotel Yekaterinburg", "Russia, Ekaterinburg, Engelsa 7")
                ));
            }
        };
    }

    /**
     * DTO для POST/PUT-запросов.
     * Небольшой вложенный класс, чтобы не создавать лишний пакет.
     */
    public static final class HotelCreateRequest {
        public String name;
        public String address;

        public HotelCreateRequest() {
        }

        public HotelCreateRequest(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }
}
