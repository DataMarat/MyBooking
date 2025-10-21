package com.mybooking.hotelservice;

import java.util.List;
import java.util.Optional;

public interface HotelService {
    List<Hotel> findAll();

    Hotel create(String name, String address);

    Optional<Hotel> update(Long id, String name, String address);

    boolean delete(Long id);
}
