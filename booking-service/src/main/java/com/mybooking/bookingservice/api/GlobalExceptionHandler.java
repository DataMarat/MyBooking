package com.mybooking.bookingservice.api;

import com.mybooking.bookingservice.logging.RequestIdMdcFilter;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Централизованный обработчик ошибок API (единый JSON формат + правильные HTTP статусы).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            DateTimeException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class
    })
    public org.springframework.http.ResponseEntity<ErrorDto> handleBadRequest(Exception ex, HttpServletRequest req) {
        String message;
        if (ex instanceof MethodArgumentNotValidException manv) {
            message = manv.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .findFirst()
                    .orElse("Validation failed");
        } else if (ex instanceof ConstraintViolationException cve) {
            message = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            if (message.isBlank()) {
                message = "Validation failed";
            }
        } else {
            message = safeMessage(ex);
        }
        return build(HttpStatus.BAD_REQUEST, "Bad Request", message, req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public org.springframework.http.ResponseEntity<ErrorDto> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", safeMessage(ex), req);
    }

    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
    public org.springframework.http.ResponseEntity<ErrorDto> handleNotFound(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", safeMessage(ex), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public org.springframework.http.ResponseEntity<ErrorDto> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", safeMessage(ex), req);
    }

    @ExceptionHandler({AuthenticationException.class, InvalidBearerTokenException.class})
    public org.springframework.http.ResponseEntity<ErrorDto> handleUnauthorized(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", safeMessage(ex), req);
    }

    /**
     * 409 Conflict:
     * - конфликты уникальности/идемпотентности
     * - конкурентные записи и т.п.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public org.springframework.http.ResponseEntity<ErrorDto> handleConflict(DataIntegrityViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict", "Conflict", req);
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ErrorDto> handleFallback(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", safeMessage(ex), req);
    }

    private org.springframework.http.ResponseEntity<ErrorDto> build(HttpStatus status, String error, String message, HttpServletRequest req) {
        String traceId = MDC.get(RequestIdMdcFilter.MDC_TRACE_ID);
        ErrorDto body = new ErrorDto(
                Instant.now(),
                status.value(),
                error,
                message,
                req.getRequestURI(),
                traceId
        );
        return org.springframework.http.ResponseEntity.status(status).body(body);
    }

    private String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank()) ? ex.getClass().getSimpleName() : msg;
    }
}
