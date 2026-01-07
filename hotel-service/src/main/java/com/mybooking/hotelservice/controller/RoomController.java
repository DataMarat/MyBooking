package com.mybooking.hotelservice.controller;

import com.mybooking.hotelservice.dto.RoomDetailsDto;
import com.mybooking.hotelservice.model.Room;
import com.mybooking.hotelservice.model.RoomReservationLock;
import com.mybooking.hotelservice.service.HotelService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.mybooking.hotelservice.dto.RoomListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * REST-контроллер для управления номерами отелей.
 *
 * <p>Обрабатывает запросы, связанные с CRUD-операциями над номерами,
 * а также операции удержания, подтверждения и освобождения доступности номеров.</p>
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final HotelService hotelService;

    public RoomController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    /**
     * Возвращает пагинированный список номеров без вложенной информации об отеле.
     *
     * @return список номеров
     */
    @GetMapping
    public Page<RoomListDto> listRooms(Pageable pageable) {
        return hotelService.listRooms(pageable)
                .map(r -> new RoomListDto(
                        r.getId(),
                        r.getHotel() != null ? r.getHotel().getId() : null,
                        r.getNumber(),
                        r.getCapacity(),
                        r.getTimesBooked(),
                        r.isAvailable()
                ));
    }

    /**
     * Возвращает номер по идентификатору.
     *
     * @param id идентификатор номера
     * @return номер
     */
    @GetMapping("/{id}")
    public RoomDetailsDto getRoom(@PathVariable Long id) {
        Room room = hotelService.getRoom(id)
                .orElseThrow(() -> new IllegalStateException("Room not found"));

        return new RoomDetailsDto(
                room.getId(),
                room.getHotel() != null ? room.getHotel().getId() : null,
                room.getNumber(),
                room.getCapacity(),
                room.getTimesBooked(),
                room.isAvailable()
        );
    }

    /**
     * Создаёт или обновляет номер.
     *
     * @param room данные номера
     * @return сохранённый номер
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public Room saveRoom(@RequestBody Room room) {
        return hotelService.saveRoom(room);
    }

    /**
     * Удаляет номер по идентификатору.
     *
     * @param id идентификатор номера
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public void deleteRoom(@PathVariable Long id) {
        hotelService.deleteRoom(id);
    }

    /**
     * Создаёт удержание номера на указанный период.
     *
     * @param requestId идентификатор запроса
     * @param roomId идентификатор номера
     * @param startDate дата начала
     * @param endDate дата окончания
     * @return удержание номера
     */
    @PostMapping("/{roomId}/hold")
    public RoomReservationLock holdRoom(
            @RequestParam String requestId,
            @PathVariable Long roomId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return hotelService.holdRoom(requestId, roomId, startDate, endDate);
    }

    /**
     * Подтверждает ранее созданное удержание номера.
     *
     * @param requestId идентификатор запроса
     * @return подтверждённое удержание
     */
    @PostMapping("/confirm")
    public RoomReservationLock confirmHold(@RequestParam String requestId) {
        return hotelService.confirmHold(requestId);
    }

    /**
     * Освобождает ранее созданное удержание номера.
     *
     * @param requestId идентификатор запроса
     * @return освобождённое удержание
     */
    @PostMapping("/release")
    public RoomReservationLock releaseHold(@RequestParam String requestId) {
        return hotelService.releaseHold(requestId);
    }
}
