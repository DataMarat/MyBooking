package com.mybooking.hotelservice.config;

import com.mybooking.hotelservice.model.Hotel;
import com.mybooking.hotelservice.model.Room;
import com.mybooking.hotelservice.repository.HotelRepository;
import com.mybooking.hotelservice.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Предзаполнение БД из CSV для демонстрации бизнес-сценариев.
 *
 * <p>Загружает данные только если таблицы hotels и rooms пустые.
 * Источники: classpath:data/hotels.csv и classpath:data/rooms.csv.</p>
 *
 * <p>Связь Room -> Hotel строится через hotel_id из CSV.</p>
 */
@Component
public class CsvDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CsvDataInitializer.class);

    private static final String HOTELS_CSV = "data/hotels.csv";
    private static final String ROOMS_CSV = "data/rooms.csv";

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public CsvDataInitializer(HotelRepository hotelRepository, RoomRepository roomRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long hotelsCount = hotelRepository.count();
        long roomsCount = roomRepository.count();

        if (hotelsCount > 0 || roomsCount > 0) {
            log.info("CSV prefill skipped: tables are not empty (hotels={}, rooms={}).", hotelsCount, roomsCount);
            return;
        }

        Map<Long, Hotel> hotelsById = loadHotels();
        loadRooms(hotelsById);

        log.info("CSV prefill completed: hotels={}, rooms={}.", hotelRepository.count(), roomRepository.count());
    }

    private Map<Long, Hotel> loadHotels() throws IOException {
        Map<Long, Hotel> hotelsById = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(HOTELS_CSV).getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                throw new IllegalStateException("Hotels CSV is empty: " + HOTELS_CSV);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = splitCsvLine(line, 4, HOTELS_CSV);

                Long id = Long.parseLong(parts[0].trim());
                String name = parts[1].trim();
                String city = parts[2].trim();
                String address = parts[3].trim();

                Hotel hotel = new Hotel();
                hotel.setId(id);
                hotel.setName(name);
                hotel.setCity(city);
                hotel.setAddress(address);

                Hotel saved = hotelRepository.save(hotel);
                hotelsById.put(saved.getId(), saved);
            }
        }

        return hotelsById;
    }

    private void loadRooms(Map<Long, Hotel> hotelsById) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(ROOMS_CSV).getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                throw new IllegalStateException("Rooms CSV is empty: " + ROOMS_CSV);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = splitCsvLine(line, 6, ROOMS_CSV);

                Long id = Long.parseLong(parts[0].trim());
                Long hotelId = Long.parseLong(parts[1].trim());
                String number = parts[2].trim();
                int capacity = Integer.parseInt(parts[3].trim());
                long timesBooked = Long.parseLong(parts[4].trim());
                boolean available = Boolean.parseBoolean(parts[5].trim());

                Hotel hotel = hotelsById.get(hotelId);
                if (hotel == null) {
                    throw new IllegalStateException("Unknown hotel_id '" + hotelId + "' in " + ROOMS_CSV);
                }

                Room room = new Room();
                room.setId(id);
                room.setHotel(hotel);
                room.setNumber(number);
                room.setCapacity(capacity);
                room.setTimesBooked(timesBooked);
                room.setAvailable(available);

                roomRepository.save(room);
            }
        }
    }

    /**
     * Упрощённый CSV-парсер (без кавычек/экранирования).
     */
    private static String[] splitCsvLine(String line, int expectedColumns, String source) {
        String[] parts = line.split(",", -1);
        if (parts.length != expectedColumns) {
            throw new IllegalArgumentException(
                    "Invalid CSV format in " + source + ": expected " + expectedColumns +
                            " columns but got " + parts.length + ". Line: " + line
            );
        }
        return parts;
    }
}
