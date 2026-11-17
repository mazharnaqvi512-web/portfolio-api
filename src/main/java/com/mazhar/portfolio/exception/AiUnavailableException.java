package com.mazhar.portfolio.exception;

/** The assistant cannot answer because it is not set up on this server (no API key). Maps to 503. */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }
}
