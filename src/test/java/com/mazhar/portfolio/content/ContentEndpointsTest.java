package com.mazhar.portfolio.content;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** The real services, loading the real JSON, through the real controllers — no mocks. */
@SpringBootTest
@AutoConfigureMockMvc
class ContentEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesTheProfile() throws Exception {
        mockMvc.perform(get("/api/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name", is("Syed Mazhar Hussain")))
            .andExpect(jsonPath("$.data.role", is("Senior Angular Developer")))
            .andExpect(jsonPath("$.data.yearsOfExperience", is(5)));
    }

    @Test
    void servesEveryProjectAndFindsOneBySlug() throws Exception {
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(7)))
            .andExpect(jsonPath("$.data[0].slug", is("meal-dynamics")));

        mockMvc.perform(get("/api/projects/fleet-management-system"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name", is("Fleet Management System (FMS)")))
            .andExpect(jsonPath("$.data.keyFeatures", hasSize(7)));
    }

    @Test
    void omitsOptionalProjectFieldsThatAreUnsetButSendsTheOnesThatAreSet() throws Exception {
        mockMvc.perform(get("/api/projects/meal-dynamics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.liveDemoUrl").doesNotExist())
            .andExpect(jsonPath("$.data.thumbnail").doesNotExist());

        mockMvc.perform(get("/api/projects/tabib-group"))
            .andExpect(jsonPath("$.data.liveDemoUrl", is("https://tabibgroup.net/")));
    }

    @Test
    void unknownProjectIsA404() throws Exception {
        mockMvc.perform(get("/api/projects/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void servesTheWorkHistoryNewestFirstWithTheCurrentRolesEndDateAsAnExplicitNull() throws Exception {
        mockMvc.perform(get("/api/experience"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(3)))
            .andExpect(jsonPath("$.data[0].company", is("Bajco Technologies")))
            .andExpect(jsonPath("$.data[0].endDate", nullValue()))
            .andExpect(jsonPath("$.data[1].endDate", is("2025-04")))
            .andExpect(jsonPath("$.data[2].projectLinks[0].label", is("Tabib Group")));
    }

    @Test
    void sectionsWithoutContentYetAreEmptyListsNotErrors() throws Exception {
        mockMvc.perform(get("/api/case-studies")).andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get("/api/blog")).andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(0)));
    }
}
