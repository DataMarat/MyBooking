package com.mybooking.hotelservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import java.util.List;

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

//    /**
//     * Предзаполнение базы данных 5 отелями при старте.
//     * Теперь также заполняем city и stars.
//     */
//    @Bean
//    CommandLineRunner initData(HotelRepository repository) {
//        return args -> {
//            if (repository.count() == 0) {
//                repository.saveAll(List.of(
//                        new Hotel(null, "Cosmos Nevsky",
//                                "Russia, St. Petersburg, Nevsky Ave 171",
//                                "St. Petersburg", 4),
//                        new Hotel(null, "Ibis Kazan Center",
//                                "Russia, Kazan, Pravo-Bulachnaya 43/1",
//                                "Kazan", 3),
//                        new Hotel(null, "Azimut Moscow Olympic",
//                                "Russia, Moscow, Olimpiyskiy Ave 18/1",
//                                "Moscow", 4),
//                        new Hotel(null, "Park Inn Sochi City",
//                                "Russia, Sochi, Gorkogo 56",
//                                "Sochi", 3),
//                        new Hotel(null, "Novotel Yekaterinburg",
//                                "Russia, Ekaterinburg, Engelsa 7",
//                                "Yekaterinburg", 4)
//                ));
//            }
//        };
//    }
//
//    /** DTO для POST/PUT-запросов */
//    public static final class HotelCreateRequest {
//        public String name;
//        public String address;
//        public String city;
//        public Integer stars;
//
//        public HotelCreateRequest() { }
//
//        public HotelCreateRequest(String name, String address, String city, Integer stars) {
//            this.name = name;
//            this.address = address;
//            this.city = city;
//            this.stars = stars;
//        }
//    }
}
