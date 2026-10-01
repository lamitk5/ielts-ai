package com.ieltsaitutor.rag.admin;

import java.time.Instant;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ieltsaitutor.rag.ingestion.RagInvalidStateException;
import com.ieltsaitutor.rag.ingestion.RagValidationException;

@RestControllerAdvice(assignableTypes = RagAdminController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RagAdminExceptionHandler {
    @ExceptionHandler(RagInvalidStateException.class)
    ResponseEntity<ErrorResponse> invalidState(RagInvalidStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                new ErrorResponse.Error(exception.getCode(), exception.getMessage()), Instant.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                new ErrorResponse.Error("RAG_INVALID_REQUEST", "Dữ liệu RAG chưa hợp lệ."), Instant.now()));
    }

    @ExceptionHandler(RagValidationException.class)
    ResponseEntity<ErrorResponse> validation(RagValidationException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                new ErrorResponse.Error(exception.code(), exception.getMessage()), Instant.now()));
    }

    public record ErrorResponse(Error error, Instant timestamp) {
        public record Error(String code, String message) {}
    }
}
