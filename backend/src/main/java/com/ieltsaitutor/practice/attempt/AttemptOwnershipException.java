package com.ieltsaitutor.practice.attempt;

public class AttemptOwnershipException extends RuntimeException {
    public AttemptOwnershipException() { super("Attempt is not available"); }
}
