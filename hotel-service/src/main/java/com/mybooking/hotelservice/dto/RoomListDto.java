package com.mybooking.hotelservice.dto;

/**
 * DTO для списка номеров (без вложенного Hotel), чтобы не раздувать JSON и избежать рекурсии.
 */
public record RoomListDto(
        Long id,
        Long hotelId,
        String number,
        int capacity,
        long timesBooked,
        boolean available
) {}
