package com.mazhar.portfolio.config;

/** The "prod" profile is active but no usable database URL was provided. */
public class MissingProductionDatabaseException extends RuntimeException {

    public MissingProductionDatabaseException(String message) {
        super(message);
    }
}
