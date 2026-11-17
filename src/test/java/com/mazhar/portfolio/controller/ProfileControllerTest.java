package com.mazhar.portfolio.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.mazhar.portfolio.dto.ProfileDto;
import com.mazhar.portfolio.service.ProfileService;

/** @WebMvcTest only wires the web layer, so the service is mocked here rather than relying on its real bean. */
@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @Test
    void returnsProfileWrappedInSuccessEnvelope() throws Exception {
        given(profileService.getProfile()).willReturn(new ProfileDto(
            "Syed Mazhar Hussain",
            "Senior Angular Developer",
            "Building scalable, high-performance enterprise applications.",
            5,
            "Become an Enterprise Full Stack Engineer.",
            "Enterprise Frontend Development",
            List.of("Java", "Spring Boot"),
            "Available for Remote Opportunities"
        ));

        mockMvc.perform(get("/api/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.name", is("Syed Mazhar Hussain")))
            .andExpect(jsonPath("$.data.role", is("Senior Angular Developer")));
    }
}
