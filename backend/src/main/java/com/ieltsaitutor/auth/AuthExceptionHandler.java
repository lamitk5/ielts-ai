package com.ieltsaitutor.auth;

import java.time.Instant;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {
    @ExceptionHandler(AuthException.class)
    ResponseEntity<ErrorResponse> auth(AuthException exception) {
        return ResponseEntity.status(exception.status()).body(new ErrorResponse(
                new ErrorResponse.Error(exception.code(), exception.getMessage()), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                new ErrorResponse.Error("AUTH_INVALID_REQUEST", "Thông tin đăng nhập chưa hợp lệ."), Instant.now()));
    }

    public record ErrorResponse(Error error, Instant timestamp) {
        public record Error(String code, String message) {}
    }
}
