package com.mybooking.bookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа Booking Service проекта MyBooking.
 *
 * <p>Сервис отвечает за управление бронированиями и выдачу JWT токенов для аутентификации пользователей.</p>
 */
@SpringBootApplication
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
