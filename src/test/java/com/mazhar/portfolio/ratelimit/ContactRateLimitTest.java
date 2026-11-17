package com.mazhar.portfolio.ratelimit;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.mazhar.portfolio.controller.ContactController;
import com.mazhar.portfolio.service.ContactService;

@WebMvcTest(value = ContactController.class, properties = {
    "contact.rate-limit.max-requests=2",
    "contact.rate-limit.window=10m"
})
class ContactRateLimitTest {

    private static final String VALID_BODY = """
        {
          "name": "Jane Doe",
          "email": "jane@example.com",
          "subject": "Project inquiry",
          "message": "A message that is definitely long enough to pass validation."
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContactService contactService;

    private static RequestPostProcessor fromAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    @Test
    void rejectsTheRequestOverTheLimitWithA429AndRetryAfter() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/contact").with(fromAddress("10.0.0.1"))
                    .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/contact").with(fromAddress("10.0.0.1"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.message", is("Too many requests. Please try again later.")));
    }

    @Test
    void limitsEachClientAddressIndependently() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/contact").with(fromAddress("10.0.0.2"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));
        }

        mockMvc.perform(post("/api/contact").with(fromAddress("10.0.0.2"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isTooManyRequests());
        mockMvc.perform(post("/api/contact").with(fromAddress("10.0.0.3"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isOk());
    }

    @Test
    void countsInvalidSubmissionsSoBotsCannotProbeWithMalformedBodies() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/contact").with(fromAddress("10.0.0.4"))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/contact").with(fromAddress("10.0.0.4"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void doesNotCountCorsPreflightRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(options("/api/contact").with(fromAddress("10.0.0.5"))
                    .header("Origin", "http://localhost:4200")
                    .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/contact").with(fromAddress("10.0.0.5"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isOk());
    }
}
