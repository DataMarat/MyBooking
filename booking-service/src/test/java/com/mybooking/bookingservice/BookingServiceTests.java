package com.mybooking.bookingservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mybooking.bookingservice.model.Booking;
import com.mybooking.bookingservice.repository.BookingRepository;
import com.mybooking.bookingservice.service.BookingService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Сервисные тесты booking-service (MVC + RestClient).
 *
 * <p>hotel-service эмулируется WireMock на динамическом порту.</p>
 *
 * <p>Важно: WireMock должен стартовать ДО вычисления @DynamicPropertySource,
 * иначе невозможно вычислить hotel.base-url.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingServiceTests {

    private static final WireMockServer WIREMOCK = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        // Стартуем WireMock на этапе загрузки класса, чтобы port() был доступен в @DynamicPropertySource
        WIREMOCK.start();
        configureFor("localhost", WIREMOCK.port());
    }

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("hotel.base-url", () -> "http://localhost:" + WIREMOCK.port());
        r.add("hotel.timeout-ms", () -> "800");
        r.add("hotel.retries", () -> "1");

        // отключаем Eureka в тестах
        r.add("eureka.client.enabled", () -> "false");
        r.add("eureka.client.register-with-eureka", () -> "false");
        r.add("eureka.client.fetch-registry", () -> "false");
    }

    @AfterAll
    void stopWiremock() {
        // Останавливаем один раз после всех тестов класса
        WIREMOCK.stop();
    }

    @BeforeEach
    void reset() {
        WIREMOCK.resetAll();
        bookingRepository.deleteAll();
    }

    @Test
    void successFlow_confirmed() {
        stubFor(post(urlPathMatching("/api/rooms/\\d+/hold"))
                .withQueryParam("requestId", matching(".+"))
                .withQueryParam("startDate", matching(".+"))
                .withQueryParam("endDate", matching(".+"))
                .willReturn(aResponse().withStatus(200)));

        stubFor(post(urlPathEqualTo("/api/rooms/confirm"))
                .withQueryParam("requestId", matching(".+"))
                .willReturn(aResponse().withStatus(200)));

        Booking b = bookingService.createBooking(
                1L, 10L,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                "r1"
        );

        Assertions.assertEquals(Booking.Status.CONFIRMED, b.getStatus());
        Assertions.assertNotNull(b.getId());
    }

    @Test
    void failureFlow_cancelledWithCompensation() {
        stubFor(post(urlPathMatching("/api/rooms/\\d+/hold"))
                .willReturn(serverError()));

        stubFor(post(urlPathEqualTo("/api/rooms/release"))
                .withQueryParam("requestId", matching(".+"))
                .willReturn(aResponse().withStatus(200)));

        Booking b = bookingService.createBooking(
                2L, 11L,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                "r2"
        );

        Assertions.assertEquals(Booking.Status.CANCELLED, b.getStatus());
        Assertions.assertNotNull(b.getId());
    }

    @Test
    void timeoutFlow_cancelled() {
        stubFor(post(urlPathMatching("/api/rooms/\\d+/hold"))
                .willReturn(aResponse().withFixedDelay(2_000).withStatus(200)));

        stubFor(post(urlPathEqualTo("/api/rooms/release"))
                .withQueryParam("requestId", matching(".+"))
                .willReturn(aResponse().withStatus(200)));

        Booking b = bookingService.createBooking(
                3L, 12L,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                "r3"
        );

        Assertions.assertEquals(Booking.Status.CANCELLED, b.getStatus());
    }

    @Test
    void idempotency_noDuplicate() {
        stubFor(post(urlPathMatching("/api/rooms/\\d+/hold"))
                .willReturn(aResponse().withStatus(200)));

        stubFor(post(urlPathEqualTo("/api/rooms/confirm"))
                .withQueryParam("requestId", equalTo("r4"))
                .willReturn(aResponse().withStatus(200)));

        Booking b1 = bookingService.createBooking(
                4L, 13L,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                "r4"
        );

        Booking b2 = bookingService.createBooking(
                4L, 13L,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                "r4"
        );

        Assertions.assertEquals(b1.getId(), b2.getId());
        Assertions.assertEquals(1L, bookingRepository.count());
    }

    @Test
    void suggestions_sorted() {
        stubFor(get(urlPathEqualTo("/api/rooms"))
                .willReturn(okJson("""
                        [
                          {"id": 1, "number": "101", "timesBooked": 5},
                          {"id": 2, "number": "102", "timesBooked": 1}
                        ]
                        """)));

        List<BookingService.RoomView> rooms = bookingService.getRoomSuggestions();

        Assertions.assertEquals(2, rooms.size());
        Assertions.assertEquals(1L, rooms.get(0).id());
        Assertions.assertEquals(2L, rooms.get(1).id());
    }
}
