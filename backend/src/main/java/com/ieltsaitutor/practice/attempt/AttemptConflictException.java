package com.ieltsaitutor.practice.attempt;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AttemptConflictException extends RuntimeException {
    public AttemptConflictException(String message) { super(message); }
}
