package com.mazhar.portfolio.exception;

/** The conversation is well-formed JSON but not a valid chat (wrong turn order). Maps to 400. */
public class InvalidChatRequestException extends RuntimeException {

    public InvalidChatRequestException(String message) {
        super(message);
    }
}
