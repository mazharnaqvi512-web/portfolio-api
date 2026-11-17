package com.mazhar.portfolio.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.mock.env.MockEnvironment;

class ProductionDatabaseGuardTest {

    private final ProductionDatabaseGuard guard = new ProductionDatabaseGuard();
    private final SpringApplication application = new SpringApplication();

    private MockEnvironment prod() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }

    @Test
    void refusesProdWithNoDatabaseUrlAtAll() {
        assertThatThrownBy(() -> guard.postProcessEnvironment(prod(), application))
            .isInstanceOf(MissingProductionDatabaseException.class)
            .hasMessageContaining("SPRING_DATASOURCE_URL");
    }

    @Test
    void refusesProdWhenTheUrlIsAnUnresolvedPlaceholder() {
        // What application-prod.properties produces when the environment variable is missing.
        MockEnvironment environment = prod().withProperty("spring.datasource.url", "${SPRING_DATASOURCE_URL}");

        assertThatThrownBy(() -> guard.postProcessEnvironment(environment, application))
            .isInstanceOf(MissingProductionDatabaseException.class);
    }

    @Test
    void refusesProdWithABlankOrNonJdbcUrl() {
        assertThatThrownBy(() -> guard.postProcessEnvironment(prod().withProperty("spring.datasource.url", ""), application))
            .isInstanceOf(MissingProductionDatabaseException.class);
        assertThatThrownBy(() -> guard.postProcessEnvironment(
            prod().withProperty("spring.datasource.url", "postgres://user:pw@host/db"), application))
            .isInstanceOf(MissingProductionDatabaseException.class);
    }

    @Test
    void allowsProdWithARealJdbcUrl() {
        MockEnvironment environment = prod().withProperty("spring.datasource.url", "jdbc:postgresql://db.internal:5432/portfolio");

        assertThatCode(() -> guard.postProcessEnvironment(environment, application)).doesNotThrowAnyException();
    }

    @Test
    void leavesEveryOtherProfileAlone() {
        assertThatCode(() -> guard.postProcessEnvironment(new MockEnvironment(), application)).doesNotThrowAnyException();
    }

    @Test
    void runsAfterTheConfigurationFilesHaveBeenLoaded() {
        assertThat(guard.getOrder()).isEqualTo(org.springframework.core.Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void theFailureReportNamesTheMissingSettingAndHowToFixIt() {
        MissingProductionDatabaseException cause = new MissingProductionDatabaseException(ProductionDatabaseGuard.MESSAGE);

        FailureAnalysis analysis = new ProductionDatabaseFailureAnalyzer().analyze(cause);

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("SPRING_DATASOURCE_URL", "erases");
        assertThat(analysis.getAction()).contains("SPRING_DATASOURCE_USERNAME", "SPRING_PROFILES_ACTIVE=prod");
    }
}
