package com.mybooking.bookingservice.controller;

import com.mybooking.bookingservice.model.User;
import com.mybooking.bookingservice.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * Административный REST-контроллер управления пользователями.
 *
 * <p>Предназначен для административных операций (например, просмотр/удаление пользователей).
 * Доступ ограничивается на уровне security/ролей.</p>
 */


@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {
    private final UserRepository userRepository;

    public UserAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    /**
     * Возвращает список пользователей.
     *
     * <p>Административная операция. Доступ ограничивается настройками безопасности.</p>
     *
     * @return список пользователей
     */
    public List<User> list() { return userRepository.findAll(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<User> get(@PathVariable Long id) {
        return userRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    /**
     * Обновляет пользователя.
     *
     * <p>Административная операция. Пароль ожидается уже в виде хеша (если обновляется),
     * либо должен быть обработан согласно принятой политике безопасности.</p>
     *
     * @param id идентификатор пользователя
     * @param user новые данные пользователя
     * @return обновлённый пользователь
     */
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User u) {
        return userRepository.findById(id)
                .map(ex -> { u.setId(id); return ResponseEntity.ok(userRepository.save(u)); })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    /**
     * Удаляет пользователя по идентификатору.
     *
     * <p>Административная операция. Использовать с осторожностью, т.к. может нарушить целостность
     * связанных данных (бронирования и т.д.).</p>
     *
     * @param id идентификатор пользователя
     */
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
