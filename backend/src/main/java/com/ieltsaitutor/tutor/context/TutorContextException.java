package com.ieltsaitutor.tutor.context;

public class TutorContextException extends RuntimeException {
    private final String code;
    private final int status;

    public TutorContextException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public int getStatus() { return status; }
}
