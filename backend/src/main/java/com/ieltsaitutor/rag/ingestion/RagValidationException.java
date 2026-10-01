package com.ieltsaitutor.rag.ingestion;

public class RagValidationException extends RuntimeException {
    private final String code;

    public RagValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
