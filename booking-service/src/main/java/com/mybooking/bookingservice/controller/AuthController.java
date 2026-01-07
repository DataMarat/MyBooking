package com.mybooking.bookingservice.controller;

import com.mybooking.bookingservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST-контроллер для регистрации и аутентификации пользователей.
 *
 * <p>Обе операции возвращают JWT токен доступа.</p>
 */
@RestController
@RequestMapping("/api/user")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Регистрирует пользователя и возвращает JWT.
     *
     * @param request параметры регистрации
     * @return JSON с access_token и token_type
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        String token = authService.register(request.username(), request.password(), request.admin());
        return ResponseEntity.ok(tokenResponse(token));
    }

    /**
     * Аутентифицирует пользователя и возвращает JWT.
     *
     * @param request параметры аутентификации
     * @return JSON с access_token и token_type
     */
    @PostMapping("/auth")
    public ResponseEntity<Map<String, String>> auth(@RequestBody AuthRequest request) {
        String token = authService.login(request.username(), request.password());
        return ResponseEntity.ok(tokenResponse(token));
    }

    private static Map<String, String> tokenResponse(String token) {
        return Map.of(
                "access_token", token,
                "token_type", "Bearer"
        );
    }

    /**
     * Запрос регистрации.
     *
     * <p>Поле admin опционально; если не передано, по умолчанию false.</p>
     */
    public record RegisterRequest(String username, String password, boolean admin) { }

    /**
     * Запрос аутентификации.
     */
    public record AuthRequest(String username, String password) { }
}
