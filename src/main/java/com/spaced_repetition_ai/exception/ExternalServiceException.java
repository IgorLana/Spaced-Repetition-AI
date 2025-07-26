package com.spaced_repetition_ai.exception;

public class ExternalServiceException extends RuntimeException{
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
