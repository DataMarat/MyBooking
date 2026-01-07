package com.mybooking.bookingservice.api;

/**
 * Исключение бизнес-уровня: логин уже занят.
 *
 * <p>Используется при регистрации пользователя, чтобы возвращать корректный HTTP 409.</p>
 */
public class UsernameAlreadyExistsException extends IllegalStateException {

    public UsernameAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}
