# MyBooking

MyBooking — учебная микросервисная система бронирования отелей на **Spring Boot 3.x** и **Java 17**.  
Проект демонстрирует: **Service Discovery (Eureka)**, **API Gateway (Spring Cloud Gateway)**, **JWT-аутентификацию**, а также **согласованное управление состоянием** бронирования между сервисами через двухшаговую сагу **hold → confirm / release**.

---

## Архитектура

### Компоненты

- **eureka-server** — Service Registry (Eureka).
- **api-gateway** — единая точка входа для клиентских запросов (часть маршрутов).
- **hotel-service** — отели/номера, статистика, операции удержания доступности номера (hold/confirm/release).
- **booking-service** — регистрация/аутентификация, CRUD бронирований, оркестрация саги с hotel-service.

### Диаграмма взаимодействий

```mermaid
flowchart LR
  Client[Client / Postman] -->|HTTP + Bearer JWT| GW[api-gateway :8080]

  GW -->|/api/hotels/**| HS[hotel-service :8081]
  GW -->|/api/bookings/**<br/>/api/user/**| BS[booking-service :8082]

  BS -->|hold / confirm / release<br/>HTTP (base-url)| HS

  HS -->|register| ES[eureka-server :8761]
  BS -->|register| ES
  GW -->|discover| ES
```

**Важно про маршрутизацию:** в текущей конфигурации Gateway публикует только:
- `/api/hotels/**` → `hotel-service`
- `/api/bookings/**`, `/api/user/**` → `booking-service`

Эндпойнты `hotel-service` для номеров (`/api/rooms/**`) и статистики (`/api/stats`) доступны **напрямую** через `http://localhost:8081`.

---

## Порты и окружение

| Сервис | Порт |
|---|---:|
| eureka-server | 8761 |
| api-gateway | 8080 |
| hotel-service | 8081 |
| booking-service | 8082 |

Все сервисы используют **H2 in-memory** в dev-режиме — данные сбрасываются при перезапуске.

---

## Быстрый старт

### Требования

- Java 17+
- Maven или Maven Wrapper

### Сборка

```bash
./mvnw clean package
```

### Запуск сервисов

Запускайте в отдельных терминалах (или используйте `run-all.ps1` на Windows):

```bash
./mvnw -pl eureka-server spring-boot:run
./mvnw -pl hotel-service spring-boot:run
./mvnw -pl booking-service spring-boot:run
./mvnw -pl api-gateway spring-boot:run
```

### Smoke-check

- Eureka UI: `http://localhost:8761`
- Health:
  - `GET http://localhost:8080/actuator/health`
  - `GET http://localhost:8081/actuator/health`
  - `GET http://localhost:8082/actuator/health`

---

## Безопасность

### JWT

Аутентификация реализована в `booking-service` и используется как Bearer JWT:

- секрет для подписи: `security.jwt.secret` (в обоих сервисах одинаковый; dev-значение в `application.yml`)
- срок действия токена: 1 час (см. реализацию генерации токена в `booking-service`)
- авторизация основана на `scope` (`USER` / `ADMIN`) и Spring Security authorities (`SCOPE_USER`, `SCOPE_ADMIN`)

### Заголовок Authorization

```
Authorization: Bearer <access_token>
```

---

## API

Ниже приведены **фактические** пути, соответствующие контроллерам проекта.

### 1) Аутентификация и пользователи (booking-service, через Gateway)

База: `http://localhost:8080`

#### Регистрация
`POST /api/user/register`

```json
{
  "username": "user1",
  "password": "password",
  "admin": false
}
```

Ответ:

```json
{
  "access_token": "…",
  "token_type": "Bearer"
}
```

#### Аутентификация
`POST /api/user/auth`

```json
{
  "username": "user1",
  "password": "password"
}
```

### 2) Бронирования (booking-service, через Gateway)

База: `http://localhost:8080`

#### Создать бронирование (USER)
`POST /api/bookings`

Тело:

```json
{
  "roomId": "1",
  "startDate": "2026-01-10",
  "endDate": "2026-01-12"
}
```

#### Мои бронирования (USER)
`GET /api/bookings`

#### Подборка рекомендаций номеров (USER)
`GET /api/bookings/suggestions`

> Возвращает список рекомендуемых номеров, используемых для выбора (сортировка по загруженности / доступности определяется реализацией сервисного слоя).

#### Все бронирования (ADMIN)
`GET /api/bookings/all`

### 3) Администрирование пользователей (booking-service, напрямую)

База: `http://localhost:8082`

> Эти эндпойнты не маршрутизируются через Gateway в текущей конфигурации.

- `GET /api/admin/users` (ADMIN)
- `GET /api/admin/users/{id}` (ADMIN)
- `PUT /api/admin/users/{id}` (ADMIN)
- `DELETE /api/admin/users/{id}` (ADMIN)

### 4) Отели (hotel-service, через Gateway)

База: `http://localhost:8080`

- `GET /api/hotels` — пагинированный список (DTO без rooms)
- `GET /api/hotels/{id}` — детальная карточка (DTO с rooms)
- `POST /api/hotels` — создать/обновить
- `DELETE /api/hotels/{id}` — удалить

Пример пагинации:

`GET /api/hotels?page=0&size=10`

### 5) Номера и статистика (hotel-service, напрямую)

База: `http://localhost:8081`

#### Номера (CRUD)
- `GET /api/rooms` — пагинация
- `GET /api/rooms/{id}`
- `POST /api/rooms`
- `DELETE /api/rooms/{id}`

Пример пагинации:

`GET /api/rooms?page=0&size=20`

#### Статистика по номерному фонду
`GET /api/stats`

Ответ:

```json
{
  "totalRooms": 32,
  "availableRooms": 32,
  "totalBookings": 0
}
```

#### Endpoints саги (service↔service)

- `POST /api/rooms/{roomId}/hold?requestId=...&startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
- `POST /api/rooms/confirm?requestId=...`
- `POST /api/rooms/release?requestId=...`

---

## Сага бронирования (booking-service ↔ hotel-service)

### Поток “успех”

1. `booking-service` вызывает `hotel-service`:
  - `POST /api/rooms/{roomId}/hold` (с `requestId`, `startDate`, `endDate`)
2. `booking-service` создаёт `Booking` со статусом `PENDING`.
3. `booking-service` вызывает:
  - `POST /api/rooms/confirm` (с тем же `requestId`)
4. При успехе `Booking` переводится в `CONFIRMED`.

### Ошибка и компенсация

Если `hold` или `confirm` завершается ошибкой (например, конфликт дат / 409 или ошибка сервиса):

- бронирование переводится в `CANCELLED`
- выполняется best-effort компенсация:
  - `POST /api/rooms/release?requestId=...`

---

## Сквозная корреляция запросов (X-Request-Id + MDC)

Для трассировки действий в рамках одного пользовательского запроса используется заголовок:

- `X-Request-Id`

`booking-service` кладёт значение в MDC как `traceId` и печатает его в логах (см. `logging.pattern.console`), что позволяет связывать:
- входящий HTTP-запрос,
- вызовы в `hotel-service`,
- изменения статуса бронирования,
- компенсационные действия.

---

## Предзаполнение данных (Test data)

Данные загружаются из CSV **при старте сервиса** (если таблицы пустые).

### hotel-service

Файлы:
- `hotel-service/src/main/resources/data/hotels.csv` — 8 записей
- `hotel-service/src/main/resources/data/rooms.csv` — 32 записи

Назначение:
- демонстрация списка отелей/номеров;
- наличие `timesBooked` для демонстрации рекомендаций/статистики.

### booking-service

Файлы:
- `booking-service/src/main/resources/data/users.csv` — 8 записей
- `booking-service/src/main/resources/data/bookings.csv` — 24 записи

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

---

## Swagger / OpenAPI

Агрегированная Swagger UI в Gateway:

- `http://localhost:8080/swagger-ui.html`

Gateway проксирует OpenAPI:
- booking-service docs: `/api/bookings/v3/api-docs`
- hotel-service docs: `/api/hotels/v3/api-docs`

---

## Тестирование

### Запуск всех тестов

```bash
./mvnw test
```

### Запуск тестов модуля

```bash
./mvnw -pl booking-service test
./mvnw -pl hotel-service test
./mvnw -pl api-gateway test
```

### Интеграционные тесты саги (WireMock)

В `booking-service` используются интеграционные тесты, которые поднимают Spring-контекст и подменяют `hotel-service` через WireMock (успех/ошибка и компенсация).

---

## ADR (Architecture Decision Records)

Ниже перечислены ключевые архитектурные решения проекта в формате ADR.

### ADR-001 — Микросервисная архитектура с Service Discovery (Eureka) и API Gateway

**Статус:** Accepted

**Контекст:** Требуется продемонстрировать распределённую архитектуру с маршрутизацией запросов и динамическим обнаружением сервисов.

**Решение:**
- Используется Eureka как реестр сервисов.
- Используется Spring Cloud Gateway как edge-сервис, маршрутизирующий часть публичных API.

**Последствия:**
- Сервисы регистрируются в Eureka и могут вызываться по `lb://<service-id>`.
- Gateway является единой точкой входа для части клиентских маршрутов.
- В текущей конфигурации через Gateway публикуются только `/api/hotels/**`, `/api/bookings/**`, `/api/user/**`.

### ADR-002 — JWT (HMAC shared secret) и проверка токена на сервисах

**Статус:** Accepted

**Контекст:** Нужна аутентификация и разграничение прав (USER/ADMIN) без внешнего Identity Provider.

**Решение:**
- `booking-service` выпускает JWT.
- `booking-service` и `hotel-service` валидируют JWT как Resource Server с общим секретом `security.jwt.secret`.

**Последствия:**
- Нет зависимости от внешнего IdP.
- Требуется синхронизация секрета между сервисами (в dev хранится в `application.yml`; для production — в секретах окружения).

### ADR-003 — Согласованность данных через сага hold/confirm/release и корреляция requestId

**Статус:** Accepted

**Контекст:** Бронирование требует проверки доступности номера и согласованного изменения состояния между двумя сервисами без распределённых транзакций.

**Решение:**
- Реализован двухшаговый протокол:
  - `hold` — временное удержание,
  - `confirm` — подтверждение,
  - `release` — компенсация.
- Во все шаги передаётся `requestId`, используемый для идемпотентности и повторной доставки.

**Последствия:**
- Устойчивость к сбоям удалённого сервиса достигается компенсацией.
- Повторные вызовы с тем же `requestId` не должны приводить к дублированию удержаний.

### ADR-004 — Сквозная трассировка через X-Request-Id и MDC

**Статус:** Accepted

**Контекст:** Нужна наблюдаемость цепочки действий (создание брони → hold/confirm/release) и сопоставление логов.

**Решение:**
- Вводится заголовок `X-Request-Id`.
- Значение пишется в MDC как `traceId` и выводится в логах.

**Последствия:**
- Упрощается диагностика саги и интеграционных ошибок.
- Клиент может повторять запросы, сохраняя `X-Request-Id`.

---
