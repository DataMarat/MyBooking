package com.mybooking.bookingservice.model;

import jakarta.persistence.*;
/**
 * JPA-сущность пользователя.
 *
 * <p>Используется для регистрации и аутентификации. Пароль хранится в виде хеша (BCrypt),
 * роль используется для формирования scope в JWT.</p>
 */


@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String passwordHash;
    private String role; // роль пользователя: USER или ADMIN

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}


