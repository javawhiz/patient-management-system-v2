package com.clinic.pms.exception;

/** Thrown when a requested entity (patient, history entry, etc.) does not exist. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
