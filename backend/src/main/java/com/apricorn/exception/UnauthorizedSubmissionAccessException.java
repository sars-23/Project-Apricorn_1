package com.apricorn.exception;

public class UnauthorizedSubmissionAccessException extends RuntimeException {
    public UnauthorizedSubmissionAccessException(String message) {
        super(message);
    }
}
