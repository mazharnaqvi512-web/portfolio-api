package com.mazhar.portfolio.exception;

/** The assistant's global daily call cap is used up. Maps to 429. */
public class AiDailyLimitException extends RuntimeException {

    public AiDailyLimitException() {
        super("The assistant's daily limit has been reached.");
    }
}
