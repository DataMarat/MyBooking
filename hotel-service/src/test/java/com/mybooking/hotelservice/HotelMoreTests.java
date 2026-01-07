package com.mybooking.hotelservice;

import com.mybooking.hotelservice.model.Hotel;
import com.mybooking.hotelservice.model.Room;
import com.mybooking.hotelservice.model.RoomReservationLock;
import com.mybooking.hotelservice.repository.HotelRepository;
import com.mybooking.hotelservice.repository.RoomReservationLockRepository;
import com.mybooking.hotelservice.service.HotelService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Дополнительные интеграционные тесты Hotel Service.
 *
 * <p>Проверяют конфликт удержаний по пересечению диапазонов дат, корректность освобождения удержаний,
 * а также поведение при истёкших удержаниях.</p>
 */
@SpringBootTest
public class HotelMoreTests {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomReservationLockRepository lockRepository;

    @Autowired
    private HotelService hotelService;

    @Test
    @Transactional
    void conflictOnOverlappingHold() {
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

        LocalDate s = LocalDate.now().plusDays(1);
        LocalDate e = s.plusDays(3);

        hotelService.holdRoom("req-a", r.getId(), s, e);

        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> hotelService.holdRoom("req-b", r.getId(), s.plusDays(1), e.plusDays(1))
        );
        Assertions.assertTrue(ex.getMessage().toLowerCase().contains("unavailable"));
    }

    @Test
    @Transactional
    void releaseAllowsNewHold() {
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

        LocalDate s = LocalDate.now().plusDays(1);
        LocalDate e = s.plusDays(2);

        hotelService.holdRoom("req-1", r.getId(), s, e);
        hotelService.releaseHold("req-1");

        RoomReservationLock lock2 = hotelService.holdRoom("req-2", r.getId(), s, e);
        Assertions.assertEquals(RoomReservationLock.Status.HELD, lock2.getStatus());
    }

    @Test
    @Transactional
    void expiredHoldCannotBeConfirmed() {
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

        LocalDate s = LocalDate.now().plusDays(1);
        LocalDate e = s.plusDays(2);

        RoomReservationLock lock = hotelService.holdRoom("req-exp", r.getId(), s, e);

        lock.setExpiresAt(LocalDate.now().minusDays(1));
        lockRepository.save(lock);

        Assertions.assertThrows(IllegalStateException.class, () -> hotelService.confirmHold("req-exp"));
    }
}
