package com.mybooking.hotelservice.model;

import jakarta.persistence.*;

/**
 * JPA-сущность номера отеля.
 *
 * <p>Представляет отдельный номер, принадлежащий конкретному отелю.
 * Сущность используется как для операций управления номерным фондом,
 * так и для аналитики загрузки.</p>
 */
@Entity
public class Room {

    /**
     * Уникальный идентификатор номера.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Номер комнаты (например: "101", "A-203").
     *
     * <p>Используется как человекочитаемый идентификатор номера
     * внутри конкретного отеля.</p>
     */
    private String number;

    /**
     * Максимальное количество гостей, которое может быть размещено в номере.
     */
    private int capacity;

    /**
     * Счётчик количества бронирований номера.
     *
     * <p>Используется для сбора статистики и анализа популярности номеров.</p>
     */
    private long timesBooked;

    /**
     * Признак доступности номера.
     *
     * <p>Используется для быстрого исключения номера из выборок,
     * если он временно недоступен (например, на ремонте).</p>
     */
    private boolean available = true;

    /**
     * Отель, к которому относится номер.
     *
     * <p>Связь многие-к-одному. Загрузка ленивaя,
     * так как информация об отеле не всегда требуется при работе с номером.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private Hotel hotel;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getNumber() { return number; }

    public void setNumber(String number) { this.number = number; }

    public int getCapacity() { return capacity; }

    public void setCapacity(int capacity) { this.capacity = capacity; }

    public long getTimesBooked() { return timesBooked; }

    public void setTimesBooked(long timesBooked) { this.timesBooked = timesBooked; }

    public boolean isAvailable() { return available; }

    public void setAvailable(boolean available) { this.available = available; }

    public Hotel getHotel() { return hotel; }

    public void setHotel(Hotel hotel) { this.hotel = hotel; }
}
