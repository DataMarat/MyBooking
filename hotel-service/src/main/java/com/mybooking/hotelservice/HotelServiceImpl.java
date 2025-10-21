package com.mybooking.hotelservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class HotelServiceImpl implements HotelService {

    private final HotelRepository repo;

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> findAll() {
        return repo.findAll();
    }

    @Override
    public Hotel create(String name, String address) {
        return repo.save(new Hotel(null, name.trim(), address.trim()));
    }

    @Override
    public Optional<Hotel> update(Long id, String name, String address) {
        return repo.findById(id).map(existing -> {
            existing.setName(name.trim());
            existing.setAddress(address.trim());
            return repo.save(existing);
        });
    }

    @Override
    public boolean delete(Long id) {
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        return true;
    }
}
