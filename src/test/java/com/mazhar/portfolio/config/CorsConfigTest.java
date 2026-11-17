package com.mazhar.portfolio.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import com.mazhar.portfolio.controller.ProjectController;
import com.mazhar.portfolio.service.ProjectService;

@WebMvcTest(value = ProjectController.class, properties = {
    "cors.allowed-origins=https://example.com, https://www.example.com"
})
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    private ResultActions preflightFrom(String origin) throws Exception {
        return mockMvc.perform(options("/api/projects")
            .header("Origin", origin)
            .header("Access-Control-Request-Method", "GET"));
    }

    @Test
    void allowsEveryConfiguredOriginIgnoringSurroundingWhitespace() throws Exception {
        preflightFrom("https://example.com")
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://example.com"));
        preflightFrom("https://www.example.com")
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://www.example.com"));
    }

    @Test
    void rejectsAnOriginThatIsNotConfigured() throws Exception {
        preflightFrom("https://evil.example.net").andExpect(status().isForbidden());
    }

    @Test
    void theDevServerIsNoLongerAllowedOnceOriginsAreConfigured() throws Exception {
        preflightFrom("http://localhost:4200").andExpect(status().isForbidden());
    }
}
