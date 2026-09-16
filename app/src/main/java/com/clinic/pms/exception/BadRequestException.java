package com.clinic.pms.exception;

/** Thrown when a request is well-formed but semantically invalid (e.g., bad restore file). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
