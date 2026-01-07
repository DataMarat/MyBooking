package com.mybooking.hotelservice.controller;

import com.mybooking.hotelservice.dto.HotelDetailsDto;
import com.mybooking.hotelservice.dto.HotelListDto;
import com.mybooking.hotelservice.dto.RoomDto;
import com.mybooking.hotelservice.model.Hotel;
import com.mybooking.hotelservice.service.HotelService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер для управления отелями.
 *
 * <p>Обрабатывает HTTP-запросы, связанные с операциями CRUD над сущностью {@link Hotel}.</p>
 */
@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    /**
     * Список всех отелей с использованием пагинации.
     * Возвращает "тонкий" DTO без rooms, чтобы избежать рекурсивной сериализации и лишнего payload.
     *
     * @return список отелей
     */
    @GetMapping
    public Page<HotelListDto> listHotels(Pageable pageable) {
        return hotelService.listHotels(pageable)
                .map(h -> new HotelListDto(h.getId(), h.getName(), h.getAddress(), h.getCity()));
    }

    /**
     * Детальная карточка отеля (с номерами), но номера без обратной ссылки hotel.
     *
     * @param id идентификатор отеля
     * @return отель
     */
    @GetMapping("/{id}")
    public HotelDetailsDto getHotel(@PathVariable Long id) {
        Hotel hotel = hotelService.getHotelWithRooms(id)
                .orElseThrow(() -> new IllegalStateException("Hotel not found"));

        List<RoomDto> rooms = hotel.getRooms().stream()
                .map(r -> new RoomDto(r.getId(), r.getNumber(), r.getCapacity(), r.getTimesBooked(), r.isAvailable()))
                .toList();

        return new HotelDetailsDto(hotel.getId(), hotel.getName(), hotel.getAddress(), hotel.getCity(), rooms);
    }

    /**
     * Создаёт или обновляет отель.
     *
     * @param hotel данные отеля
     * @return сохранённый отель
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public Hotel saveHotel(@RequestBody Hotel hotel) {
        return hotelService.saveHotel(hotel);
    }

    /**
     * Удаляет отель по идентификатору.
     *
     * @param id идентификатор отеля
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public void deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
    }
}
