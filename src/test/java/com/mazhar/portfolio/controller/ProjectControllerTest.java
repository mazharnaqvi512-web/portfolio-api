package com.mazhar.portfolio.controller;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.mazhar.portfolio.service.ProjectService;

/** @WebMvcTest only wires the web layer, so the service is mocked here rather than relying on its real bean. */
@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @Test
    void returnsAnEmptyListHonestlyWhenNoProjectsExistYet() throws Exception {
        given(projectService.getAll()).willReturn(List.of());

        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data", is(empty())));
    }

    @Test
    void returnsNotFoundForAnUnknownSlug() throws Exception {
        given(projectService.getBySlug("does-not-exist")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success", is(false)));
    }
}
