package com.mazhar.portfolio.exception;

/** A request body ran past the size limit while it was being read (no Content-Length to reject it up front). Maps to 413. */
public class RequestTooLargeException extends RuntimeException {

    public RequestTooLargeException() {
        super("Request body is too large.");
    }
}
