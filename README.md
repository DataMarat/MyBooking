# hotel-service

Минималистичный микросервис на Spring Boot 3 (Java 17).

## Что есть сейчас
- `GET /api/hotels` — список из 5 отелей (hardcoded).
- Логирование (Logback по умолчанию).
- Тест, проверяющий, что отелей ровно 5.
- Swagger/OpenAPI UI.

## Структура проекта
```
MyBooking/
├── pom.xml # корневой pom (multi-module)
│
└── hotel-service/ # текущий микросервис
├── pom.xml # pom модуля
├── README.md # документация
├── src/
│ ├── main/
│ │ ├── java/
│ │ │ └── com/
│ │ │ └──── mybooking/
│ │ │ └──── hotelservice/
    │   │   ├── Hotel.java
    │   │   ├── HotelRepository.java
    │   │   ├── HotelService.java
    │   │   ├── HotelServiceImpl.java
    │   │   ├── HotelController.java
    │   │   └── HotelServiceApplication.java 
│ │ └── resources/
│ │ └──── application.yml
│ │
│ └── test/
│ └──── java/
│ └────── com/
│ └──────── mybooking/
│ └──────────── hotelservice/
│ └──────────────── HotelServiceApplicationTests.java
│
└── target/ # сборочные артефакты (генерируется автоматически)
```
## Быстрый старт

```bash
# из каталога модуля
cd hotel-service

# запустить приложение
mvn spring-boot:run
```

Приложение слушает порт 8050.

Полезные URL’ы

API: http://localhost:8050/api/hotels

Swagger UI: http://localhost:8050/swagger-ui

OpenAPI JSON: http://localhost:8050/v3/api-docs

Примеры запросов
```bash
# Получить список отелей
curl -s http://localhost:8050/api/hotels | jq .
```

Ожидаемый ответ (пример):
```json
[
  {"id":1,"name":"Cosmos Nevsky","address":"Russia, St. Petersburg, Nevsky Ave 171"},
  {"id":2,"name":"Ibis Kazan Center","address":"Russia, Kazan, Pravo-Bulachnaya 43/1"},
  {"id":3,"name":"Azimut Moscow Olympic","address":"Russia, Moscow, Olimpiyskiy Ave 18/1"},
  {"id":4,"name":"Park Inn Sochi City","address":"Russia, Sochi, Gorkogo 56"},
  {"id":5,"name":"Novotel Yekaterinburg","address":"Russia, Ekaterinburg, Engelsa 7"}
]
```
# GET    http://localhost:8050/api/hotels
# POST   http://localhost:8050/api/hotels         {"name":"Hotel Test","address":"City, Street 1"}
# PUT    http://localhost:8050/api/hotels/1       {"name":"Updated","address":"Updated"}
# DELETE http://localhost:8050/api/hotels/1

Тесты
```bash
mvn test
```

Troubleshooting

Порт занят → поменяйте порт в src/main/resources/application.yml:

```yaml
server:
  port: 8050
```

Swagger UI не открывается → убедитесь, что сервис запущен и переходите на /swagger-ui (без /index.html).
