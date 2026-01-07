package com.mybooking.hotelservice.repository;

import com.mybooking.hotelservice.model.RoomReservationLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностью {@link RoomReservationLock}.
 *
 * <p>Предоставляет методы для поиска удержаний по идентификатору запроса (идемпотентность)
 * и для проверки конфликтующих удержаний/подтверждений по пересечению диапазонов дат.</p>
 */
public interface RoomReservationLockRepository extends JpaRepository<RoomReservationLock, Long> {

    /**
     * Возвращает удержание по идентификатору запроса.
     *
     * @param requestId идентификатор запроса
     * @return удержание, если найдено
     */
    Optional<RoomReservationLock> findByRequestId(String requestId);

    /**
     * Возвращает удержания для номера, удовлетворяющие условиям:
     * статус входит в переданный список и диапазон дат пересекается с указанным.
     *
     * @param roomId         идентификатор номера
     * @param statuses       допустимые статусы удержаний
     * @param endInclusive   верхняя граница пересечения (включительно)
     * @param startInclusive нижняя граница пересечения (включительно)
     * @return список конфликтующих удержаний
     */
    List<RoomReservationLock> findByRoomIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long roomId,
            List<RoomReservationLock.Status> statuses,
            LocalDate endInclusive,
            LocalDate startInclusive
    );
}
