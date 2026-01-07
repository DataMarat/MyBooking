package com.mybooking.bookingservice.repository;

import com.mybooking.bookingservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/**
 * Репозиторий пользователей.
 *
 * <p>Инкапсулирует доступ к данным пользователей и предоставляет методы
 * для поиска пользователя по логину (username).</p>
 */


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    /**
     * Проверяет наличие пользователя по логину.
     *
     * @param username логин
     * @return true, если пользователь с таким логином уже существует
     */
    boolean existsByUsername(String username);
}
