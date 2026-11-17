package com.mazhar.portfolio.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Turns {@link MissingProductionDatabaseException} into Spring Boot's readable "APPLICATION FAILED TO START" block. */
public class ProductionDatabaseFailureAnalyzer extends AbstractFailureAnalyzer<MissingProductionDatabaseException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MissingProductionDatabaseException cause) {
        return new FailureAnalysis(
            cause.getMessage() + ". Without it the app would fall back to a local file database that a hosting platform erases on every restart.",
            "Set SPRING_DATASOURCE_URL (and SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD if the database needs them) "
                + "in the host's environment variables, or start without SPRING_PROFILES_ACTIVE=prod for local development.",
            cause);
    }
}
