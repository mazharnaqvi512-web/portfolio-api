package com.mazhar.portfolio.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/**
 * Stops the app, before anything else starts, when the "prod" profile is active without a real
 * database URL. Hosting platforms usually wipe the disk on every deploy or restart, so running on
 * the local H2 file there would silently lose every stored contact message.
 *
 * <p>Registered in META-INF/spring.factories. It has to run after the configuration files are
 * loaded, hence the lowest precedence. An unset SPRING_DATASOURCE_URL is easy to miss: Spring
 * would otherwise carry on with the literal text "${SPRING_DATASOURCE_URL}" as the URL and fail
 * later with an error that does not name the missing setting.
 */
public class ProductionDatabaseGuard implements EnvironmentPostProcessor, Ordered {

    static final String MESSAGE =
        "The 'prod' profile needs SPRING_DATASOURCE_URL set to a JDBC URL, for example "
            + "jdbc:postgresql://HOST:5432/DATABASE";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        if (!looksLikeJdbcUrl(urlOf(environment))) {
            throw new MissingProductionDatabaseException(MESSAGE);
        }
    }

    private static String urlOf(ConfigurableEnvironment environment) {
        try {
            return environment.getProperty("spring.datasource.url");
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            return null;
        }
    }

    private static boolean looksLikeJdbcUrl(String url) {
        return url != null && url.startsWith("jdbc:");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
