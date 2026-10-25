package com.ceu.pefima.backend.advisor;

/**
 * Raised when a posted profile cannot be analysed. Messages never contain values.
 */
public class InvalidProfileException extends RuntimeException {

    public InvalidProfileException(String message) {
        super(message);
    }

}
