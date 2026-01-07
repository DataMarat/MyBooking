package com.mybooking.bookingservice.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
/**
 * JPA-сущность бронирования.
 *
 * <p>Хранит состояние бронирования в booking-service и содержит данные,
 * необходимые для корреляции с операциями удержания/подтверждения номера в hotel-service
 * (идемпотентность по requestId, статусы и временные метки).</p>
 */


@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_booking_request", columnNames = {"requestId"}))
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId; // ключ идемпотентности запроса

    private Long userId;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String correlationId; // идентификатор корреляции для логирования

    private OffsetDateTime createdAt;

    public enum Status { PENDING, CONFIRMED, CANCELLED }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
