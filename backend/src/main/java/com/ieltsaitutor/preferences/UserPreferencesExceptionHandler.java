package com.ieltsaitutor.preferences;

import java.time.Instant;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserPreferencesController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserPreferencesExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ignored) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                new ErrorResponse.Error("PREFERENCES_INVALID_REQUEST", "Thiết lập chưa hợp lệ."), Instant.now()));
    }

    public record ErrorResponse(Error error, Instant timestamp) {
        public record Error(String code, String message) {}
    }
}
