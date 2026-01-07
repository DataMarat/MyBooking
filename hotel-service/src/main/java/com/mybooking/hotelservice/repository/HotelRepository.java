package com.mybooking.hotelservice.repository;

import com.mybooking.hotelservice.model.Hotel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/**
 * Репозиторий для сущности {@link Hotel}.
 *
 * <p>Предоставляет стандартные CRUD-операции Spring Data JPA для работы с отелями.</p>
 */
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    @EntityGraph(attributePaths = "rooms")
    Optional<Hotel> findWithRoomsById(Long id);
}