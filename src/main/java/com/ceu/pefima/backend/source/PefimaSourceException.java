package com.ceu.pefima.backend.source;

/**
 * Raised when a PeFiMa data source is missing, incompatible, or returns data
 * that breaks its contract. Messages never contain source values.
 */
public class PefimaSourceException extends RuntimeException {

    public PefimaSourceException(String message) {
        super(message);
    }

    public PefimaSourceException(String message, Throwable cause) {
        super(message, cause);
    }

}
