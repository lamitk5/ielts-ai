package com.ieltsaitutor.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class AiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        return error(HttpStatus.BAD_REQUEST, "AI_INVALID_REQUEST", "Tin nhắn hoặc ngữ cảnh chưa hợp lệ.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "AI_INVALID_REQUEST", "Dữ liệu yêu cầu chưa hợp lệ.");
    }

    @ExceptionHandler(AiProviderException.class)
    ResponseEntity<ApiErrorResponse> handleProvider(AiProviderException exception) {
        return error(exception.status(), exception.code(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        return error(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR", "Trợ giảng AI chưa thể trả lời lúc này.");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                new ApiErrorResponse.Error(code, message), Instant.now()));
    }

    public record ApiErrorResponse(Error error, Instant timestamp) {
        public record Error(String code, String message) { }
    }
}
