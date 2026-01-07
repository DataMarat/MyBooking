package com.mybooking.hotelservice.service;

import com.mybooking.hotelservice.model.Hotel;
import com.mybooking.hotelservice.model.Room;
import com.mybooking.hotelservice.model.RoomReservationLock;
import com.mybooking.hotelservice.repository.HotelRepository;
import com.mybooking.hotelservice.repository.RoomRepository;
import com.mybooking.hotelservice.repository.RoomReservationLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Сервисный слой Hotel Service.
 *
 * <p>Содержит операции управления отелями и номерами, а также операции удержания,
 * подтверждения и освобождения доступности номера на период дат по идентификатору запроса.</p>
 */
@Service
public class HotelService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomReservationLockRepository lockRepository;

    public HotelService(HotelRepository hotelRepository, RoomRepository roomRepository, RoomReservationLockRepository lockRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.lockRepository = lockRepository;
    }

    public List<Hotel> listHotels() { return hotelRepository.findAll(); }
    public Optional<Hotel> getHotel(Long id) { return hotelRepository.findById(id); }
    public Hotel saveHotel(Hotel h) { return hotelRepository.save(h); }
    public void deleteHotel(Long id) { hotelRepository.deleteById(id); }

    public List<Room> listRooms() { return roomRepository.findAll(); }
    public Page<Room> listRooms(Pageable pageable) {
        return roomRepository.findAll(pageable);
    }
    public Optional<Room> getRoom(Long id) { return roomRepository.findById(id); }
    public Room saveRoom(Room r) { return roomRepository.save(r); }
    public void deleteRoom(Long id) { roomRepository.deleteById(id); }

    public Page<Hotel> listHotels(Pageable pageable) {
        return hotelRepository.findAll(pageable);
    }

    public Optional<Hotel> getHotelWithRooms(Long id) {
        return hotelRepository.findWithRoomsById(id);
    }

    @Transactional
    public RoomReservationLock holdRoom(String requestId, Long roomId, LocalDate startDate, LocalDate endDate) {
        Optional<RoomReservationLock> existing = lockRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            return existing.get();
        }
        List<RoomReservationLock> conflicts = lockRepository
                .findByRoomIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        roomId,
                        Arrays.asList(RoomReservationLock.Status.HELD, RoomReservationLock.Status.CONFIRMED),
                        endDate,
                        startDate
                );
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Room unavailable");
        }
        RoomReservationLock lock = new RoomReservationLock();
        lock.setRequestId(requestId);
        lock.setRoomId(roomId);
        lock.setStartDate(startDate);
        lock.setEndDate(endDate);
        lock.setStatus(RoomReservationLock.Status.HELD);
        lock.setExpiresAt(LocalDate.now().plusDays(1));
        return lockRepository.save(lock);
    }

    @Transactional
    public RoomReservationLock confirmHold(String requestId) {
        RoomReservationLock lock = lockRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Hold not found"));
        if (lock.getStatus() == RoomReservationLock.Status.CONFIRMED) {
            return lock; // идемпотентность
        }
        if (lock.getStatus() == RoomReservationLock.Status.RELEASED) {
            throw new IllegalStateException("Hold already released");
        }
        if (lock.getExpiresAt() != null && lock.getExpiresAt().isBefore(LocalDate.now())) {
            lock.setStatus(RoomReservationLock.Status.RELEASED);
            lockRepository.save(lock);
            throw new IllegalStateException("Hold expired");
        }
        lock.setStatus(RoomReservationLock.Status.CONFIRMED);
        return lockRepository.save(lock);
    }

    @Transactional
    public RoomReservationLock releaseHold(String requestId) {
        RoomReservationLock lock = lockRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Hold not found"));
        if (lock.getStatus() == RoomReservationLock.Status.RELEASED) {
            return lock; // идемпотентность
        }
        if (lock.getStatus() == RoomReservationLock.Status.CONFIRMED) {
            return lock; // уже подтверждено; ничего не делаем для идемпотентности
        }
        lock.setStatus(RoomReservationLock.Status.RELEASED);
        return lockRepository.save(lock);
    }
}
