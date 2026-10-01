package com.ieltsaitutor.learning.draft;

public class DraftConflictException extends RuntimeException {
    private final String code;

    public DraftConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
