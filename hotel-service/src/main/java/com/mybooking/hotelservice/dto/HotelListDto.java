package com.mybooking.hotelservice.dto;

/**
 * DTO для списка отелей (без номеров).
 * Используется в GET /api/hotels, чтобы не отдавать агрегат целиком и не ловить рекурсию.
 */
public record HotelListDto(Long id, String name, String address, String city) {}
