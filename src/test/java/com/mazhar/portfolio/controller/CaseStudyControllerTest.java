package com.mazhar.portfolio.controller;

import static org.hamcrest.Matchers.empty;
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
import com.mazhar.portfolio.service.CaseStudyService;

@WebMvcTest(CaseStudyController.class)
class CaseStudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CaseStudyService caseStudyService;

    @Test
    void returnsAnEmptyListHonestlyWhenNoCaseStudiesExistYet() throws Exception {
        given(caseStudyService.getAll()).willReturn(List.of());

        mockMvc.perform(get("/api/case-studies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data", is(empty())));
    }
}
