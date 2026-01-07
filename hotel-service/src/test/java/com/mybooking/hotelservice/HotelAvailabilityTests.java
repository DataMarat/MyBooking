package com.mybooking.hotelservice;

import com.mybooking.hotelservice.model.Hotel;
import com.mybooking.hotelservice.model.Room;
import com.mybooking.hotelservice.model.RoomReservationLock;
import com.mybooking.hotelservice.repository.HotelRepository;
import com.mybooking.hotelservice.service.HotelService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Интеграционные тесты сценариев удержания доступности номера.
 *
 * <p>Проверяет идемпотентность операций удержания и подтверждения по {@code requestId},
 * а также корректность переходов статусов удержания.</p>
 */
@SpringBootTest
public class HotelAvailabilityTests {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HotelService hotelService;

    @Test
    @Transactional
    void holdConfirmIdempotent() {
        Hotel h = new Hotel();
        h.setName("H");
        h.setCity("C");
        h.setAddress("A");

        Room r = new Room();
        r.setNumber("101");
        r.setCapacity(2);
        r.setHotel(h);
        h.getRooms().add(r);

        hotelRepository.save(h);

        String req = "req-1";
        LocalDate s = LocalDate.now().plusDays(1);
        LocalDate e = s.plusDays(2);

        RoomReservationLock l1 = hotelService.holdRoom(req, r.getId(), s, e);
        RoomReservationLock l2 = hotelService.holdRoom(req, r.getId(), s, e);
        Assertions.assertEquals(l1.getId(), l2.getId());

        hotelService.confirmHold(req);
        hotelService.confirmHold(req);

        RoomReservationLock afterConfirm = hotelService.confirmHold(req);
        Assertions.assertEquals(RoomReservationLock.Status.CONFIRMED, afterConfirm.getStatus());

        // Освобождение после подтверждения должно быть no-op согласно реализации
        RoomReservationLock afterRelease = hotelService.releaseHold(req);
        Assertions.assertEquals(RoomReservationLock.Status.CONFIRMED, afterRelease.getStatus());
    }
}
