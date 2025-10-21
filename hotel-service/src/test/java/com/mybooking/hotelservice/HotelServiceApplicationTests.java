package com.mybooking.hotelservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты в стиле AAA (Arrange-Act-Assert).
 * Перед каждым тестом пересоздаём контекст -> H2 заново с предзаполнением 5 отелей.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class HotelServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    /* ============ GET /api/hotels ============ */

    @Test
    @DisplayName("GET /api/hotels — позитивный: возвращает 5 отелей")
    void getHotels_returnsFive() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @DisplayName("GET /api/hotels — негативный: 406 при Accept: application/xml")
    void getHotels_notAcceptable_whenXmlRequested() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/hotels").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    /* ============ POST /api/hotels ============ */

    @Test
    @DisplayName("POST /api/hotels — позитивный: создаёт и увеличивает количество до 6")
    void postHotel_createsNew() throws Exception {
        // Arrange
        String json = """
                {"name":"Hotel Test","address":"Test City, Test Street 1"}
                """;
        // Act + Assert
        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/hotels/")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Hotel Test"))
                .andExpect(jsonPath("$.address").value("Test City, Test Street 1"));

        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    @DisplayName("POST /api/hotels — негативный: 400 при пустых полях")
    void postHotel_badRequest_whenInvalid() throws Exception {
        // Arrange
        String json = """
                {"name":"   ","address":""}
                """;
        // Act + Assert
        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    /* ============ PUT /api/hotels/{id} ============ */

    @Test
    @DisplayName("PUT /api/hotels/{id} — позитивный: обновляет существующий отель")
    void putHotel_updatesExisting() throws Exception {
        // Arrange: возьмём id=1 (после предзаполнения он существует)
        String json = """
                {"name":"Updated Name","address":"Updated Address"}
                """;

        // Act + Assert
        mockMvc.perform(put("/api/hotels/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.address").value("Updated Address"));
    }

    @Test
    @DisplayName("PUT /api/hotels/{id} — негативный: 404 для несуществующего id")
    void putHotel_notFound_whenMissing() throws Exception {
        // Arrange
        String json = """
                {"name":"Anything","address":"Anywhere"}
                """;

        // Act + Assert
        mockMvc.perform(put("/api/hotels/{id}", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    /* ============ DELETE /api/hotels/{id} ============ */

    @Test
    @DisplayName("DELETE /api/hotels/{id} — позитивный: удаляет существующий отель и возвращает 204")
    void deleteHotel_removesExisting() throws Exception {
        // Act + Assert
        mockMvc.perform(delete("/api/hotels/{id}", 1))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    @DisplayName("DELETE /api/hotels/{id} — негативный: 404 для несуществующего id")
    void deleteHotel_notFound_whenMissing() throws Exception {
        // Act + Assert
        mockMvc.perform(delete("/api/hotels/{id}", 9999))
                .andExpect(status().isNotFound());
    }
}
