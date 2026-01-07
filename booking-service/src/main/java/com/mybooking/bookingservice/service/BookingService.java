package com.mybooking.bookingservice.service;

import com.mybooking.bookingservice.logging.RequestIdMdcFilter;
import com.mybooking.bookingservice.model.Booking;
import com.mybooking.bookingservice.repository.BookingRepository;
import com.mybooking.bookingservice.http.RequestIdRestClientInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Сервис бизнес-логики бронирования.
 *
 * <p>Отвечает за:
 * <ul>
 *   <li>создание бронирования и идемпотентность по requestId,</li>
 *   <li>оркестрацию взаимодействия с Hotel Service (hold/confirm/release),</li>
 *   <li>получение подсказок по комнатам (room suggestions).</li>
 * </ul>
 * </p>
 *
 * <p>Реализация синхронная (MVC-стек). Для обращения к hotel-service используется {@link RestClient}.</p>
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final RestClient restClient;

    private final int retries;
    private final Duration timeout;

    /**
     * Создаёт сервис бронирований и настраивает HTTP-клиент для вызовов hotel-service.
     *
     * <p>Таймаут применяется как connect/read timeout на уровне HTTP request factory.</p>
     *
     * @param bookingRepository репозиторий бронирований
     * @param hotelBaseUrl базовый URL hotel-service (например, http://localhost:8081)
     * @param timeoutMs таймаут HTTP-вызовов к hotel-service в миллисекундах
     * @param retries количество повторов при временных ошибках
     */
    public BookingService(
            BookingRepository bookingRepository,
            @Value("${hotel.base-url}") String hotelBaseUrl,
            @Value("${hotel.timeout-ms}") int timeoutMs,
            @Value("${hotel.retries}") int retries
    ) {
        this.bookingRepository = bookingRepository;
        this.retries = retries;
        this.timeout = Duration.ofMillis(timeoutMs);

        // Настройка таймаутов для синхронного HTTP-клиента
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(timeoutMs);
        rf.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(hotelBaseUrl)
                .requestFactory(rf)
                .requestInterceptor(new RequestIdRestClientInterceptor())
                .build();
    }

    /**
     * Создаёт бронирование и инициирует резервирование номера в Hotel Service.
     *
     * <p>Контракт с Hotel Service соответствует endpoints вида <code>/api/*</code>:
     * <ul>
     *   <li>POST <code>/api/rooms/{roomId}/hold?requestId=.&startDate=.&endDate=.</code></li>
     *   <li>POST <code>/api/rooms/confirm?requestId=.</code></li>
     *   <li>POST <code>/api/rooms/release?requestId=.</code> (компенсация)</li>
     * </ul>
     * </p>
     *
     * <p>Алгоритм:
     * <ol>
     *   <li>Проверка идемпотентности по requestId</li>
     *   <li>Создание записи PENDING</li>
     *   <li>Hold в hotel-service</li>
     *   <li>Confirm в hotel-service</li>
     *   <li>Перевод бронирования в CONFIRMED</li>
     * </ol>
     * При ошибке — best-effort release и перевод в CANCELLED.</p>
     *
     * @param userId id пользователя
     * @param roomId id комнаты
     * @param start дата начала
     * @param end дата окончания
     * @param requestId requestId для идемпотентности (обязателен)
     * @return созданное (или ранее созданное) бронирование
     */
    @Transactional
    public Booking createBooking(Long userId, Long roomId, LocalDate start, LocalDate end, String requestId) {
        // traceId для логов и корреляции — это X-Request-Id (идемпотентность тоже по нему)
        final String traceId = requestId;

        String correlationId = UUID.randomUUID().toString();

        // Идемпотентность: если запрос с таким requestId уже обработан — возвращаем существующую запись
        Booking existing = bookingRepository.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            return existing;
        }

        Booking booking = new Booking();
        booking.setRequestId(requestId);
        booking.setUserId(userId);
        booking.setRoomId(roomId);
        booking.setStartDate(start);
        booking.setEndDate(end);
        booking.setStatus(Booking.Status.PENDING);
        booking.setCorrelationId(correlationId);
        booking.setCreatedAt(java.time.OffsetDateTime.now());
        booking = bookingRepository.save(booking);

        // Требуемое событие: создание PENDING
        log.info("[{}] booking status=PENDING bookingId={} roomId={} start={} end={}",
                traceId, booking.getId(), roomId, start, end);

        try {
            // Hold
            log.info("[{}] call hotel hold roomId={} requestId={}", traceId, roomId, requestId);
            holdRoom(roomId, requestId, start, end);

            // Confirm availability
            log.info("[{}] call hotel confirm-availability requestId={}", traceId, requestId);
            confirmHold(requestId);

            booking.setStatus(Booking.Status.CONFIRMED);
            bookingRepository.save(booking);

            // Требуемое событие: CONFIRMED
            log.info("[{}] booking status=CONFIRMED bookingId={}", traceId, booking.getId());
        } catch (Exception e) {
            log.warn("[{}] booking flow failed: bookingId={}, reason={}", traceId, booking.getId(), e.toString());

            // Компенсация (release) best-effort
            try {
                log.info("[{}] call hotel release requestId={}", traceId, requestId);
                releaseHold(requestId);
            } catch (Exception ignored) {
                // best-effort: не маскируем исходную причину
            }

            booking.setStatus(Booking.Status.CANCELLED);
            bookingRepository.save(booking);

            // Требуемое событие: CANCELLED
            log.info("[{}] booking status=CANCELLED bookingId={}", traceId, booking.getId());
        }

        return booking;
    }

    /**
     * DTO для подсказок, собираемый на основе данных Hotel Service.
     *
     * <p>Поля соответствуют контракту выдачи комнат hotel-service.</p>
     */
    public record RoomView(Long id, String number, long timesBooked) {}

    /**
     * Возвращает список комнат (из Hotel Service), отсортированный по популярности.
     *
     * <p>Сортировка:
     * <ul>
     *   <li>по убыванию timesBooked</li>
     *   <li>при равенстве — по возрастанию id</li>
     * </ul>
     * </p>
     *
     * @return отсортированный список комнат
     */
    public List<RoomView> getRoomSuggestions() {
        RoomView[] rooms = executeWithRetry(() ->
                        restClient.get()
                                .uri("/api/rooms")
                                .retrieve()
                                .body(RoomView[].class),
                "getRoomSuggestions"
        );

        if (rooms == null || rooms.length == 0) {
            return List.of();
        }

        return Arrays.stream(rooms)
                .sorted(Comparator.comparingLong(RoomView::timesBooked).reversed()
                        .thenComparing(RoomView::id))
                .toList();
    }

    private void holdRoom(Long roomId, String requestId, LocalDate start, LocalDate end) {
        executeWithRetry(() -> {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/rooms/{roomId}/hold")
                            .queryParam("requestId", requestId)
                            .queryParam("startDate", start)
                            .queryParam("endDate", end)
                            .build(roomId))
                    .retrieve()
                    .toBodilessEntity();
            return null;
        }, "holdRoom");
    }

    private void confirmHold(String requestId) {
        executeWithRetry(() -> {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/rooms/confirm")
                            .queryParam("requestId", requestId)
                            .build())
                    .retrieve()
                    .toBodilessEntity();
            return null;
        }, "confirmHold");
    }

    private void releaseHold(String requestId) {
        executeWithRetry(() -> {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/rooms/release")
                            .queryParam("requestId", requestId)
                            .build())
                    .retrieve()
                    .toBodilessEntity();
            return null;
        }, "releaseHold");
    }

    /**
     * Выполняет операцию с повтором при временных ошибках.
     *
     * <p>Ретраи применяются к {@link RestClientException}. Между попытками используется
     * простой backoff (300ms, затем линейное увеличение).</p>
     *
     * @param operation операция
     * @param opName имя операции для логирования
     * @param <T> тип результата
     * @return результат операции
     */
    private <T> T executeWithRetry(Operation<T> operation, String opName) {
        RestClientException last = null;

        for (int attempt = 1; attempt <= Math.max(1, retries); attempt++) {
            try {
                return operation.run();
            } catch (RestClientException ex) {
                last = ex;

                // Спец-кейс: releaseHold + 404 = нормальный сценарий (hold мог не создаться).
                if ("releaseHold".equals(opName)
                        && ex instanceof org.springframework.web.client.HttpClientErrorException.NotFound) {
                    log.debug("releaseHold skipped: hold not found (requestId={})",
                            MDC.get(RequestIdMdcFilter.MDC_TRACE_ID));
                    return null;
                }

                log.warn("Ошибка вызова hotel-service в операции {} (попытка {}/{}): {}",
                        opName, attempt, retries, ex.toString());

                // если попытки закончились — пробрасываем дальше
                if (attempt >= retries) {
                    throw ex;
                }

                // простой backoff, чтобы не долбить сервис в лоб
                sleepBackoff(attempt);
            }
        }

        // На практике сюда не попадём, но на всякий случай:
        if (last != null) {
            throw last;
        }
        return null;
    }


    private void sleepBackoff(int attempt) {
        long baseMs = 300L;
        long delayMs = baseMs * attempt;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface Operation<T> {
        T run();
    }
}
