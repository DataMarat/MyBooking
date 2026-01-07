package com.mybooking.bookingservice.service;

import com.mybooking.bookingservice.api.UsernameAlreadyExistsException;
import com.mybooking.bookingservice.model.User;
import com.mybooking.bookingservice.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Сервис регистрации и аутентификации пользователей.
 *
 * <p>Реализует:</p>
 * <ul>
 *   <li>регистрацию пользователя с хешированием пароля (BCrypt) и выдачей JWT;</li>
 *   <li>аутентификацию по логину/паролю и выдачу JWT;</li>
 *   <li>обработку конфликта уникальности логина как бизнес-ошибку.</li>
 * </ul>
 *
 * <p>JWT подписывается симметричным ключом (HMAC), задаваемым через конфигурацию.</p>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SecretKey key;
    private final long tokenTtlSeconds;

    public AuthService(
            UserRepository userRepository,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.ttl-seconds:3600}") long tokenTtlSeconds
    ) {
        this.userRepository = userRepository;
        this.tokenTtlSeconds = tokenTtlSeconds;

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * Регистрирует пользователя и выдаёт JWT.
     *
     * <p>Проверяет уникальность логина. Дополнительно защищается от гонок:
     * при конкурентной регистрации уникальность гарантируется БД, а нарушение
     * уникального ограничения преобразуется в бизнес-ошибку.</p>
     *
     * @param username логин
     * @param password пароль в открытом виде (хешируется внутри метода)
     * @param admin признак администратора (true -> роль ADMIN, иначе USER)
     * @return JWT токен
     */
    @Transactional
    public String register(String username, String password, boolean admin) {
        validateCredentials(username, password);

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        u.setRole(admin ? "ADMIN" : "USER");

        try {
            User saved = userRepository.save(u);
            return issueToken(saved);
        } catch (DataIntegrityViolationException ex) {
            // Fallback для конкурентных запросов: уникальность гарантируется БД.
            throw new UsernameAlreadyExistsException(username);
        }
    }

    /**
     * Аутентифицирует пользователя и выдаёт JWT.
     *
     * @param username логин
     * @param password пароль
     * @return JWT токен
     */
    public String login(String username, String password) {
        validateCredentials(username, password);

        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!BCrypt.checkpw(password, u.getPasswordHash())) {
            throw new IllegalArgumentException("Bad credentials");
        }

        return issueToken(u);
    }

    /**
     * Выпускает JWT для пользователя.
     *
     * <p>В токене используются:</p>
     * <ul>
     *   <li>subject = userId</li>
     *   <li>claims: username, scope (роль)</li>
     * </ul>
     *
     * @param user пользователь
     * @return подписанный JWT
     */
    private String issueToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .addClaims(Map.of(
                        "scope", user.getRole(),
                        "username", user.getUsername()
                ))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(tokenTtlSeconds)))
                .signWith(key)
                .compact();
    }

    private static void validateCredentials(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
    }
}
