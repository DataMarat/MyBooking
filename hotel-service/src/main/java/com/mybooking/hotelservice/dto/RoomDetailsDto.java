package com.mybooking.hotelservice.dto;

/**
 * Детальный DTO номера без вложенного Hotel.
 */
public record RoomDetailsDto(
        Long id,
        Long hotelId,
        String number,
        int capacity,
        long timesBooked,
        boolean available
) {}
