package com.mybooking.bookingservice.repository;

import com.mybooking.bookingservice.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
/**
 * Репозиторий бронирований.
 *
 * <p>Инкапсулирует доступ к данным бронирований и предоставляет методы
 * для поиска по ключу идемпотентности (requestId).</p>
 */


public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByRequestId(String requestId);
    /**
     * Возвращает бронирования пользователя.
     *
     * @param userId идентификатор пользователя
     * @return список бронирований
     */
    List<Booking> findByUserId(Long userId);
}
