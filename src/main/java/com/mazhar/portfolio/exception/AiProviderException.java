package com.mazhar.portfolio.exception;

/**
 * The model provider failed (network error, HTTP error, empty reply). Maps to 502 with a fixed,
 * friendly message — the cause is for the server log only and never reaches the caller.
 */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiProviderException(String message) {
        super(message);
    }
}
