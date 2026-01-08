package com.mybooking.hotelservice.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Сущность блокировки (удержания) номера на период дат.
 *
 * <p>Используется для управления состоянием удержания номера в рамках конкретного запроса,
 * идентифицируемого {@code requestId}. Значение {@code requestId} уникально, что обеспечивает
 * идемпотентность операций на уровне базы данных.</p>
 */
@Entity
@Table(
        name = "room_reservation_lock",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lock_request", columnNames = {"request_id"})
        },
        indexes = {
                @Index(
                        name = "idx_lock_room_dates",
                        columnList = "room_id, start_date, end_date"
                )
        }
)
public class RoomReservationLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Идентификатор запроса, используемый как ключ идемпотентности.
     */
    private String requestId;

    /**
     * Идентификатор номера, для которого создаётся удержание.
     */
    private Long roomId;

    /**
     * Дата начала периода удержания/бронирования.
     */
    private LocalDate startDate;

    /**
     * Дата окончания периода удержания/бронирования.
     */
    private LocalDate endDate;

    /**
     * Статус удержания.
     */
    @Enumerated(EnumType.STRING)
    private Status status;

    /**
     * Дата истечения удержания.
     */
    private LocalDate expiresAt;

    /**
     * Возможные статусы удержания номера.
     */
    public enum Status { HELD, RELEASED, CONFIRMED }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }
}
