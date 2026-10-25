package com.ceu.pefima.backend.advisor;

/**
 * Raised when the local LLM cannot be reached or gives an unusable answer.
 * Messages never contain profile values.
 */
public class AdvisorUnavailableException extends RuntimeException {

    public AdvisorUnavailableException(String message) {
        super(message);
    }

    public AdvisorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
