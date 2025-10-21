package com.mybooking.hotelservice;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/hotels", produces = MediaType.APPLICATION_JSON_VALUE)
public class HotelController {

    private static final Logger log = LoggerFactory.getLogger(HotelController.class);
    private final HotelService service;

    /** GET /api/hotels — список всех отелей. */
    @GetMapping
    public List<Hotel> listHotels() {
        log.info("GET /api/hotels");
        return service.findAll();
    }

    /** POST /api/hotels — добавить отель. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Hotel> createHotel(@RequestBody HotelServiceApplication.HotelCreateRequest req) {
        log.info("POST /api/hotels name='{}', address='{}'", req != null ? req.name : null, req != null ? req.address : null);
        if (req == null || isBlank(req.name) || isBlank(req.address)) {
            return ResponseEntity.badRequest().build();
        }
        Hotel saved = service.create(req.name, req.address);
        return ResponseEntity.created(URI.create("/api/hotels/" + saved.getId())).body(saved);
    }

    /** PUT /api/hotels/{id} — обновить отель. */
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Hotel> updateHotel(@PathVariable("id") Long id,
                                             @RequestBody HotelServiceApplication.HotelCreateRequest req) {
        log.info("PUT /api/hotels/{} name='{}', address='{}'", id, req != null ? req.name : null, req != null ? req.address : null);
        if (req == null || isBlank(req.name) || isBlank(req.address)) {
            return ResponseEntity.badRequest().build();
        }
        return service.update(id, req.name, req.address)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** DELETE /api/hotels/{id} — удалить отель. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable("id") Long id) {
        log.info("DELETE /api/hotels/{}", id);
        boolean deleted = service.delete(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
