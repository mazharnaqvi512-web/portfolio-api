package com.mazhar.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Starts the app with the real src/main/resources configuration and the "prod" profile (the
 * regular tests use their own application.properties instead), pointing the required database at
 * an in-memory one that speaks PostgreSQL's dialect. This pins what a hosting platform sees: a
 * bare health check and nothing else exposed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(
    locations = "file:src/main/resources/application.properties",
    properties = {
        "spring.datasource.url=jdbc:h2:mem:prodcheck;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ai.anthropic.api-key="
    })
class ProductionConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    // The regular tests run on src/test/resources/application.properties, which replaces the real file, so
    // these are the tests that notice a typo or a regression in the settings that actually ship.

    @Test
    void smtpIsOnlyEverUsedOverVerifiedTls() {
        assertThat(environment.getProperty("spring.mail.properties.mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(environment.getProperty("spring.mail.properties.mail.smtp.starttls.required")).isEqualTo("true");
        assertThat(environment.getProperty("spring.mail.properties.mail.smtp.ssl.checkserveridentity")).isEqualTo("true");
    }

    @Test
    void smtpCallsAreTimeBounded() {
        for (String key : new String[] {"connectiontimeout", "timeout", "writetimeout"}) {
            assertThat(environment.getProperty("spring.mail.properties.mail.smtp." + key, Integer.class))
                .as(key).isBetween(1, 15_000);
        }
    }

    @Test
    void publicEndpointsHaveABodyCap() {
        assertThat(environment.getProperty("request.max-body-bytes", Long.class)).isBetween(1_024L, 1_048_576L);
    }

    @Test
    void theAssistantsRetriesAndSpendAreBounded() {
        assertThat(environment.getProperty("ai.anthropic.max-retries", Integer.class)).isBetween(0, 2);
        assertThat(environment.getProperty("ai.daily-cap", Integer.class)).isBetween(1, 1_000);
        assertThat(environment.getProperty("ai.rate-limit.max-requests", Integer.class)).isBetween(1, 30);
    }

    @Test
    void forwardedHeadersAreOffUnlessTheDeployerTurnsThemOn() {
        assertThat(environment.getProperty("server.forward-headers-strategy")).isEqualTo("none");
    }

    @Test
    void corsDefaultsToTheDevServerOnly() {
        assertThat(environment.getProperty("cors.allowed-origins")).isEqualTo("http://localhost:4200");
    }

    @Test
    void secretsAreNeverWrittenIntoTheFile() {
        // Resolved from environment variables, so with none set they are empty, not literal values.
        assertThat(environment.getProperty("ai.anthropic.api-key", "")).isEmpty();
        assertThat(environment.getProperty("spring.mail.password", "")).isEmpty();
    }

    @Test
    void healthCheckAnswersUpWithNoDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")))
            .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void nothingElseIsExposedByActuator() throws Exception {
        for (String endpoint : new String[] {"env", "beans", "metrics", "info", "mappings", "configprops", "loggers", "heapdump"}) {
            mockMvc.perform(get("/actuator/" + endpoint))
                .andExpect(status().isNotFound());
        }
    }

    @Test
    void theH2ConsoleIsNotReachable() throws Exception {
        mockMvc.perform(get("/h2-console")).andExpect(status().isNotFound());
    }

    @Test
    void theApiStillWorksUnderTheProdProfile() throws Exception {
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)));
    }
}
