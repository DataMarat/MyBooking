package com.mybooking.hotelservice.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Провайдер HMAC-ключа для проверки JWT.
 *
 * <p>Формирует ключ для алгоритма HmacSHA256 на основе строкового секрета.
 * При недостаточной длине секрета выполняется дополнение до минимально допустимого размера.</p>
 */
final class JwtSecretKeyProvider {

    private JwtSecretKeyProvider() {
    }

    /**
     * Возвращает HMAC-ключ для алгоритма HmacSHA256.
     *
     * @param secret строковый секрет
     * @return секретный ключ для HMAC
     */
    static SecretKey getHmacKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}
