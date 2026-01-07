package com.mybooking.hotelservice.controller;

import com.mybooking.hotelservice.model.Room;
import com.mybooking.hotelservice.repository.RoomRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST-контроллер статистики Hotel Service.
 *
 * <p>Предоставляет агрегированную информацию о состоянии номерного фонда.</p>
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final RoomRepository roomRepository;

    public StatsController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * Возвращает статистику по номерам.
     *
     * <p>Включает общее количество номеров, количество доступных номеров
     * и суммарное число бронирований.</p>
     *
     * @return карта со статистическими показателями
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public Map<String, Object> stats() {
        List<Room> rooms = roomRepository.findAll();

        long totalRooms = rooms.size();
        long availableRooms = rooms.stream().filter(Room::isAvailable).count();
        long totalBookings = rooms.stream().mapToLong(Room::getTimesBooked).sum();

        Map<String, Object> result = new HashMap<>();
        result.put("totalRooms", totalRooms);
        result.put("availableRooms", availableRooms);
        result.put("totalBookings", totalBookings);

        return result;
    }
}
