package com.mazhar.portfolio.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.mazhar.portfolio.dto.ContactRequest;
import com.mazhar.portfolio.service.ContactService;

// Limit raised so these validation tests (all from one address) never trip the rate limiter — that's covered in ContactRateLimitTest.
@WebMvcTest(value = ContactController.class, properties = "contact.rate-limit.max-requests=1000")
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContactService contactService;

    private static final String VALID_BODY = """
        {
          "name": "Jane Doe",
          "email": "jane@example.com",
          "subject": "Project inquiry",
          "message": "A message that is definitely long enough to pass validation."
        }
        """;

    @Test
    void acceptsAValidSubmission() throws Exception {
        given(contactService.submit(any(ContactRequest.class))).willReturn(true);

        mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void returnsSuccessEvenWhenTheServiceSilentlyDiscardsAHoneypotSubmission() throws Exception {
        given(contactService.submit(any(ContactRequest.class))).willReturn(false);

        mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void rejectsAMissingName() throws Exception {
        String body = """
            {
              "name": "",
              "email": "jane@example.com",
              "subject": "Project inquiry",
              "message": "A message that is definitely long enough to pass validation."
            }
            """;

        mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void rejectsAnInvalidEmail() throws Exception {
        String body = """
            {
              "name": "Jane Doe",
              "email": "not-an-email",
              "subject": "Project inquiry",
              "message": "A message that is definitely long enough to pass validation."
            }
            """;

        mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void rejectsAFieldLongerThanTheStoredColumnAllows() throws Exception {
        String body = """
            {
              "name": "Jane Doe",
              "email": "jane@example.com",
              "subject": "%s",
              "message": "A message that is definitely long enough to pass validation."
            }
            """.formatted("x".repeat(201));

        mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0]", is("subject: Subject must be at most 200 characters")));
    }

    @Test
    void rejectsATooShortMessage() throws Exception {
        String body = """
            {
              "name": "Jane Doe",
              "email": "jane@example.com",
              "subject": "Project inquiry",
              "message": "Too short"
            }
            """;

        mockMvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }
}
