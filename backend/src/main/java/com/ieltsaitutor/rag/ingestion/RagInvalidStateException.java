package com.ieltsaitutor.rag.ingestion;

public class RagInvalidStateException extends RuntimeException {
    private final String code;

    public RagInvalidStateException(String code, String message) {
        super(message);
        this.code = code;
    }

    public RagInvalidStateException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() { return code; }
}
