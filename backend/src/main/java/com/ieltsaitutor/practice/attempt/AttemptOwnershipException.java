package com.ieltsaitutor.practice.attempt;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AttemptOwnershipException extends RuntimeException {
    public AttemptOwnershipException() { super("Attempt is not available"); }
}
