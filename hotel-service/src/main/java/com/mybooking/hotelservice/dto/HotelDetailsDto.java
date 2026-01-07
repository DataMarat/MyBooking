package com.mybooking.hotelservice.dto;

import java.util.List;

/**
 * DTO детальной карточки отеля (включая номера, но без вложенного Hotel внутри Room).
 */
public record HotelDetailsDto(Long id, String name, String address, String city, List<RoomDto> rooms) {}
