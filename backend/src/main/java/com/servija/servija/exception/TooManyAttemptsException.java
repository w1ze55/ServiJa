package com.servija.servija.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TooManyAttemptsException extends RuntimeException {

    private final int remainingAttempts;
    private final LocalDateTime lockUntil;

    public TooManyAttemptsException(String message, int remainingAttempts, LocalDateTime lockUntil) {
        super(message);
        this.remainingAttempts = remainingAttempts;
        this.lockUntil = lockUntil;
    }
}
