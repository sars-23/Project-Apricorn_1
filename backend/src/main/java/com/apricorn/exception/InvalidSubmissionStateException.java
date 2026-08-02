package com.apricorn.exception;

public class InvalidSubmissionStateException extends RuntimeException {
    public InvalidSubmissionStateException(String message) {
        super(message);
    }
}
