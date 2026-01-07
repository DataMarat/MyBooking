package com.mybooking.hotelservice.repository;

import com.mybooking.hotelservice.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для сущности {@link Room}.
 *
 * <p>Интерфейс расширяет {@link JpaRepository} и предоставляет стандартные операции
 * доступа к данным (создание, чтение, обновление, удаление) для номеров отеля.</p>
 */
public interface RoomRepository extends JpaRepository<Room, Long> {
}
