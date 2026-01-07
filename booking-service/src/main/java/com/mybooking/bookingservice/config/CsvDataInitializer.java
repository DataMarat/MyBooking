package com.mybooking.bookingservice.config;

import com.mybooking.bookingservice.model.Booking;
import com.mybooking.bookingservice.model.User;
import com.mybooking.bookingservice.repository.BookingRepository;
import com.mybooking.bookingservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * CSV-предзаполнение данных для Booking Service.
 *
 * <p>Загружает users и bookings только если обе таблицы пустые.
 * CSV лежат в classpath:data/</p>
 */
@Component
public class CsvDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CsvDataInitializer.class);

    private static final String USERS_CSV = "data/users.csv";
    private static final String BOOKINGS_CSV = "data/bookings.csv";

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public CsvDataInitializer(UserRepository userRepository, BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long usersCount = userRepository.count();
        long bookingsCount = bookingRepository.count();

        if (usersCount > 0 || bookingsCount > 0) {
            log.info("CSV prefill skipped: users={}, bookings={}", usersCount, bookingsCount);
            return;
        }

        loadUsers();
        loadBookings();

        log.info("CSV prefill completed: users={}, bookings={}",
                userRepository.count(), bookingRepository.count());
    }

    private void loadUsers() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(USERS_CSV).getInputStream(),
                StandardCharsets.UTF_8))) {

            String header = reader.readLine(); // header
            if (header == null) {
                throw new IllegalStateException("Users CSV is empty: " + USERS_CSV);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] p = split(line, 4, USERS_CSV);

                Long id = Long.parseLong(p[0].trim());
                String username = p[1].trim();
                String rawPassword = p[2].trim();
                String role = p[3].trim(); // "USER" или "ADMIN"

                User user = new User();
                user.setId(id);
                user.setUsername(username);
                user.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
                user.setRole(role);

                userRepository.save(user);
            }
        }
    }

    private void loadBookings() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(BOOKINGS_CSV).getInputStream(),
                StandardCharsets.UTF_8))) {

            String header = reader.readLine(); // header
            if (header == null) {
                throw new IllegalStateException("Bookings CSV is empty: " + BOOKINGS_CSV);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // id,request_id,user_id,room_id,start_date,end_date,status
                String[] p = split(line, 7, BOOKINGS_CSV);

                Long id = Long.parseLong(p[0].trim());
                String requestId = p[1].trim();
                Long userId = Long.parseLong(p[2].trim());
                Long roomId = Long.parseLong(p[3].trim());
                LocalDate startDate = LocalDate.parse(p[4].trim());
                LocalDate endDate = LocalDate.parse(p[5].trim());
                Booking.Status status = Booking.Status.valueOf(p[6].trim());

                Booking booking = new Booking();
                booking.setId(id);
                booking.setRequestId(requestId);
                booking.setUserId(userId);
                booking.setRoomId(roomId);
                booking.setStartDate(startDate);
                booking.setEndDate(endDate);
                booking.setStatus(status);

                // correlationId используется для логирования
                booking.setCorrelationId("booking-" + id);

                // createdAt обязателен — задаём при загрузке
                booking.setCreatedAt(OffsetDateTime.now().minusDays(Math.min(id, 30)));

                bookingRepository.save(booking);
            }
        }
    }

    private static String[] split(String line, int expectedColumns, String source) {
        String[] parts = line.split(",", -1);
        if (parts.length != expectedColumns) {
            throw new IllegalArgumentException(
                    "Invalid CSV format in " + source + ": expected " + expectedColumns
                            + " columns but got " + parts.length + ". Line: " + line
            );
        }
        return parts;
    }
}
