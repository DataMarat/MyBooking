package com.mybooking.hotelservice.dto;

/**
 * DTO номера без обратной ссылки на отель — предотвращает циклы сериализации.
 */
public record RoomDto(Long id, String number, int capacity, long timesBooked, boolean available) {}
