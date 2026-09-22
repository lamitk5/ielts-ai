package com.ieltsaitutor.rag.admin;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ieltsaitutor.rag.ingestion.RagInvalidStateException;

@RestControllerAdvice
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

    public record ErrorResponse(Error error, Instant timestamp) {
        public record Error(String code, String message) {}
    }
}
