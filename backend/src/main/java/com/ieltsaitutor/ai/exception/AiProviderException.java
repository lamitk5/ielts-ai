package com.ieltsaitutor.ai.exception;

import org.springframework.http.HttpStatus;

public class AiProviderException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public AiProviderException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public AiProviderException(String code, HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }
}
