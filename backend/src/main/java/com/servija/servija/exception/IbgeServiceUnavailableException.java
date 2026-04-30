package com.servija.servija.exception;

public class IbgeServiceUnavailableException extends RuntimeException {

    public IbgeServiceUnavailableException(String message) {
        super(message);
    }

    public IbgeServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
