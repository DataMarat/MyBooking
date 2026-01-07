# MyBooking

MyBooking — учебная микросервисная система бронирования отелей на **Spring Boot 3.x** и **Java 17**.  
Проект демонстрирует построение распределённого backend‑приложения с **Service Discovery (Eureka)**, **API Gateway**, **JWT-аутентификацией** и согласованным управлением состоянием между сервисами (сага hold/confirm/release).

---

## 1. Архитектура

Система состоит из следующих микросервисов:

- **Eureka Server**  
  Сервис регистрации и обнаружения сервисов (Service Discovery).

- **API Gateway**  
  Единая точка входа. Отвечает за маршрутизацию HTTP‑запросов к backend‑сервисам.

- **Hotel Service**  
  Управление отелями и номерами. Отвечает за доступность номеров, рекомендации и операции резервирования слотов (hold/confirm/release).

- **Booking Service**  
  Регистрация и аутентификация пользователей, создание и управление бронированиями, а также координация с `hotel-service` (сага).

---

## 2. Используемые технологии

- **Java 17**
- **Spring Boot 3.x**
- **Spring Cloud**: Eureka, Gateway
- **Spring Security**: JWT, OAuth2 Resource Server
- **Spring Data JPA**
- **H2** (in-memory для разработки)
- **Maven** (multi-module)
- **WireMock** (интеграционные тесты без поднятия зависимого сервиса)

---

## 3. Структура репозитория

```
MyBooking/
  api-gateway/
  booking-service/
  hotel-service/
  eureka-server/
  pom.xml
```

---

## 4. Порты и окружение

Порты по умолчанию:

- **Eureka Server** — `8761`
- **API Gateway** — `8080`
- **Hotel Service** — `8081`
- **Booking Service** — `8082`

В dev‑режиме бизнес‑сервисы используют **H2 in-memory**, поэтому данные сбрасываются при перезапуске.

---

## 5. Запуск проекта

### 5.1 Требования
- Java 17
- Maven или Maven Wrapper

### 5.2 Полная сборка

```bash
./mvnw clean package
```

### 5.3 Запуск сервисов (в отдельных терминалах)

```bash
./mvnw -pl eureka-server spring-boot:run
./mvnw -pl hotel-service spring-boot:run
./mvnw -pl booking-service spring-boot:run
./mvnw -pl api-gateway spring-boot:run
```

### 5.4 Проверка работоспособности

- Eureka UI: `http://localhost:8761`
- Пример actuator health:
  - `GET http://localhost:8082/actuator/health`
  - `GET http://localhost:8081/actuator/health`

---

## 6. API Gateway

Gateway маршрутизирует запросы:

- `/api/hotels/**` → **hotel-service**
- `/api/bookings/**` → **booking-service**

Внутренние эндпойнты `hotel-service` (например confirm/release), которые предназначены для взаимодействия сервис↔сервис, через Gateway не публикуются (в зависимости от конфигурации маршрутов).

---

## 7. Сквозная корреляция: `X-Request-Id` + MDC

Для трассировки запросов используется заголовок **`X-Request-Id`**:

- Клиент может передать `X-Request-Id` самостоятельно.
- Если заголовка нет, его может сгенерировать `api-gateway` и пробросить дальше.
- `booking-service` кладёт значение в **MDC**, использует его в логах и сохраняет как `requestId` внутри бронирования.
- При исходящих вызовах `booking-service → hotel-service` заголовок **обязательно прокидывается**.

Это позволяет связывать:
- входящий HTTP‑запрос,
- запись в БД по бронированию,
- все исходящие вызовы в `hotel-service`,
- логи всех сервисов.

---

## 8. Аутентификация и авторизация (JWT)

В системе реализована JWT‑аутентификация:

- Пользователь может **зарегистрироваться**, указав логин/пароль.
- После успешной регистрации или аутентификации возвращается **JWT access token**.
- Токен содержит идентификатор пользователя, логин и роль/scope.
- Срок жизни токена — **1 час**. fileciteturn2file0

### 8.1 Эндпойнты пользователя (booking-service)

> Реальные пути см. в контроллерах `booking-service`. Ниже — типовой интерфейс из текущей реализации.

- `POST /api/user/register`
- `POST /api/user/auth`

Пример тела запроса (register/auth):

```json
{
  "username": "user1",
  "password": "password"
}
```

Ответ: JWT токен (строка или JSON-обёртка — зависит от реализации контроллера).

### 8.2 Использование токена

Для защищённых методов передавайте заголовок:

```
Authorization: Bearer <token>
```

---

## 9. Hotel Service API

### 9.1 Отели

База: `/api/hotels`

- `GET /api/hotels` — список отелей (обычно с пагинацией через `page/size`)
- `GET /api/hotels/{id}` — отель по id
- `POST /api/hotels` — создать/обновить
- `DELETE /api/hotels/{id}` — удалить

### 9.2 Номера

База: `/api/rooms`

- `GET /api/rooms` — список номеров
- `GET /api/rooms/{id}` — номер по id
- `POST /api/rooms` — создать/обновить
- `DELETE /api/rooms/{id}` — удалить

Дополнительно:

- Поиск свободных номеров по датам (конкретный эндпойнт зависит от реализации)
- `GET /api/rooms/recommend` — рекомендации номеров (сортировка по наименьшей загруженности)

> Для каждого номера ведётся счётчик бронирований `timesBooked`, который используется в рекомендациях. fileciteturn2file0

### 9.3 Locking API (для саги бронирования)

Эти эндпойнты вызываются из `booking-service` (сервис↔сервис):

- `POST /api/rooms/{roomId}/hold?requestId&startDate&endDate`
- `POST /api/rooms/confirm?requestId`
- `POST /api/rooms/release?requestId`

---

## 10. Booking Service API

База: `/api/bookings` (как правило, требует Bearer JWT)

- `POST /api/bookings` — создать бронирование (запускает сагу)
- `GET /api/bookings` — история бронирований текущего пользователя
- `DELETE /api/bookings/{id}` или `POST /api/bookings/{id}/cancel` — отмена (точный путь зависит от контроллера)
- Админские выборки (если включены): например `GET /api/bookings/all`

> Поддержка «автоматического выбора номера» реализуется через выбор доступного/рекомендованного номера на стороне `hotel-service` или внутри `booking-service` (в зависимости от реализации). fileciteturn2file0

---

## 11. Сага бронирования: согласованность данных между сервисами

Каждое бронирование проходит жизненный цикл:
`PENDING → CONFIRMED` или `PENDING → CANCELLED`. fileciteturn2file0

### 11.1 Happy path

При `POST /api/bookings`:

1. `booking-service` вызывает в `hotel-service`:
  - `POST /api/rooms/{roomId}/hold?requestId&startDate&endDate`
2. Создаётся бронирование со статусом `PENDING`, при этом:
  - `requestId` бронирования равен `X-Request-Id` входящего запроса
3. Затем `booking-service` вызывает:
  - `POST /api/rooms/confirm?requestId`
4. При успехе: бронирование переводится `PENDING → CONFIRMED`.

### 11.2 Ошибка и компенсация (best-effort release)

Если `hold` или `confirm` завершились ошибкой (например `409/500`):

- бронирование переводится `PENDING → CANCELLED`
- выполняется компенсация:
  - `POST /api/rooms/release?requestId`

**Best-effort правило:** если `release` возвращает `404`, это не считается фатальной ошибкой (не должно «ронять» сценарий).  
Это важно для идемпотентности/повторов и ситуаций, когда lock уже отсутствует.

---

## 12. Идемпотентность

Для защиты от повторной доставки запросов используется уникальный `requestId`.  
Повторный запрос с тем же `requestId` не должен приводить к созданию дубликатов. fileciteturn2file0

На практике `requestId` в бронировании привязывается к `X-Request-Id`, что удобно:
- клиент может ретраить запрос с тем же `X-Request-Id`,
- сервис сможет корректно распознать повтор.

---

## 13. Обработка ошибок (RestControllerAdvice)

В `booking-service` используется централизованная обработка ошибок через `@RestControllerAdvice`:

- единый формат ошибок (HTTP статус + сообщение + path/детали — зависит от реализации),
- обработка:
  - ошибок валидации,
  - некорректных параметров,
  - not found,
  - security/доступа,
  - ошибок интеграции с `hotel-service`.

Это снижает дублирование try/catch в контроллерах и делает API предсказуемым.

---

## 14. Swagger / OpenAPI

Если в конфигурации включён springdoc, документация доступна:

- Booking Service: `/swagger-ui/index.html`
- Hotel Service: `/swagger-ui/index.html` fileciteturn2file0

---

## 15. Test data (CSV предзаполнение)

В проекте используется предзаполнение данных из CSV‑файлов. Данные загружаются **автоматически при старте приложения**, но **только если таблицы пустые**. fileciteturn2file0

### 15.1 Hotel Service

CSV‑файлы:
- `hotel-service/src/main/resources/data/hotels.csv`
- `hotel-service/src/main/resources/data/rooms.csv`

Предзаполнение:
- Отели: **8 записей**
- Номера: **32 записи** fileciteturn2file0

Назначение:
- разные значения `timesBooked` для демонстрации рекомендаций,
- часть номеров может иметь `available=false` для демонстрации фильтрации,
- данные удобны для проверки эндпойнтов `/api/hotels`, `/api/rooms`, `/api/rooms/recommend`.

### 15.2 Booking Service

CSV‑файлы:
- `booking-service/src/main/resources/data/users.csv`
- `booking-service/src/main/resources/data/bookings.csv`

Предзаполнение:
- Пользователи: **8 записей**
- Бронирования: **24 записи** fileciteturn2file0

Тестовые пользователи:

| Username | Password | Role |
|---|---|---|
| admin | admin | ADMIN |
| manager | password | ADMIN |
| user1 | password | USER |
| user2 | password | USER |
| user3 | password | USER |
| user4 | password | USER |
| user5 | password | USER |
| qa | password | USER |

Примечание:
- Пароли в CSV хранятся в открытом виде и **хэшируются (BCrypt) при загрузке**.
- В БД хранится только `passwordHash`. fileciteturn2file0

Назначение bookings.csv:
- присутствуют бронирования во всех состояниях: `PENDING`, `CONFIRMED`, `CANCELLED`,
- удобно тестировать:
  - сагу (успех/ошибка/компенсация),
  - историю бронирований,
  - идемпотентность по `requestId`. fileciteturn2file0

### 15.3 Быстрая проверка после запуска

```bash
GET /api/hotels?page=0&size=10
GET /api/rooms/recommend
GET /api/bookings   # для авторизованного пользователя
```

---

## 16. Тестирование

### 16.1 Запуск всех тестов

```bash
./mvnw test
```

### 16.2 Запуск тестов конкретного модуля

```bash
./mvnw -pl booking-service test
./mvnw -pl hotel-service test
./mvnw -pl api-gateway test
```

### 16.3 Интеграционные тесты саги (booking-service + WireMock)

В `booking-service` есть интеграционные тесты саги, которые:
- поднимают контекст через `@SpringBootTest(webEnvironment = RANDOM_PORT)`;
- **не поднимают реальный `hotel-service`**;
- используют **WireMock** на случайном порту;
- переопределяют `hotel.base-url` через `@DynamicPropertySource`;
- делают реальный HTTP вызов `POST /api/bookings`;
- проверяют прокидывание `X-Request-Id` в `hold/confirm/release`.

Сценарии:
- **Saga Success**: `hold=200`, `confirm=200` → `CONFIRMED`
- **Saga Failure**: `hold=409/500` → `CANCELLED` + `release` best-effort (в т.ч. допустим `404`)

---

## 17. Примечание по идентификаторам (H2 + Hibernate)

В сущностях используется `@GeneratedValue(strategy = GenerationType.IDENTITY)`.  
При импорте CSV с явными `id` возможны особенности поведения в зависимости от диалекта/Hibernate.

Надёжный production‑подход:
- не задавать `id` вручную,
- строить связи через сгенерированные `id` или естественные ключи.

---

## 18. Сборка

Полная сборка:

```bash
./mvnw clean package
```

Сборка отдельного модуля:

```bash
./mvnw -pl booking-service -am clean package
```
