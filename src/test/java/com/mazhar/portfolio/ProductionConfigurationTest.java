package com.mazhar.portfolio;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
