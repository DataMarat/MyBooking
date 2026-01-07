package com.mybooking.bookingservice.controller;

import com.mybooking.bookingservice.logging.RequestIdMdcFilter;
import com.mybooking.bookingservice.model.Booking;
import com.mybooking.bookingservice.repository.BookingRepository;
import com.mybooking.bookingservice.service.BookingService;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
/**
 * REST-контроллер управления бронированиями.
 *
 * <p>Предоставляет API для создания и просмотра бронирований. Контроллер не содержит
 * бизнес-логики и делегирует операции в сервисный слой.</p>
 */


@RestController
@RequestMapping("/api/bookings")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearer-jwt")
public class BookingController {
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    public BookingController(BookingService bookingService, BookingRepository bookingRepository) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
    }

    @PostMapping
    /**
     * Создаёт бронирование номера.
     *
     * <p>Выполняет удержание номера в hotel-service, сохраняет бронирование и подтверждает удержание.
     * При ошибках выполняется компенсация (release) в hotel-service.</p>
     *
     * @param jwt JWT текущего пользователя (используется для userId/claims)
     * @param req входные параметры: roomId, startDate, endDate
     * @return созданное бронирование
     */
    public Booking create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = RequestIdMdcFilter.HEADER_REQUEST_ID, required = false) String requestIdHeader,
            @RequestBody Map<String, String> req
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        Long roomId = Long.valueOf(req.get("roomId"));
        LocalDate start = LocalDate.parse(req.get("startDate"));
        LocalDate end = LocalDate.parse(req.get("endDate"));

        // 1) Предпочитаем заголовок
        String requestId = (requestIdHeader != null && !requestIdHeader.isBlank())
                ? requestIdHeader
                : MDC.get(RequestIdMdcFilter.MDC_TRACE_ID);

        return bookingService.createBooking(userId, roomId, start, end, requestId);
    }

    @GetMapping
    /**
     * Возвращает список бронирований текущего пользователя.
     *
     * @param jwt JWT текущего пользователя
     * @return список бронирований пользователя
     */
    public List<Booking> myBookings(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bookingRepository.findByUserId(userId);
    }

    @GetMapping("/suggestions")
    public java.util.List<BookingService.RoomView> suggestions() {
        return bookingService.getRoomSuggestions();
    }

    @GetMapping("/all")
    /**
     * Возвращает список всех бронирований.
     *
     * <p>Административная операция. Доступ должен быть ограничен ролью/политикой безопасности.</p>
     *
     * @return список всех бронирований
     */
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public List<Booking> all() {
        return bookingRepository.findAll();
    }
}
