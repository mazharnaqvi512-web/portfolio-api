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
import com.mazhar.portfolio.dto.BlogPostDto;
import com.mazhar.portfolio.service.BlogService;

@WebMvcTest(BlogController.class)
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogService blogService;

    @Test
    void returnsAnEmptyListHonestlyWhenNoArticlesExistYet() throws Exception {
        given(blogService.getAll()).willReturn(List.of());

        mockMvc.perform(get("/api/blog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data", is(empty())));
    }

    @Test
    void returnsAPostBySlug() throws Exception {
        given(blogService.getBySlug("hello")).willReturn(Optional.of(new BlogPostDto(
            "1", "hello", "Hello", "Excerpt", "angular", List.of("angular"),
            "2026-01-01", null, 4, "Syed Mazhar Hussain", null, true, "published", "Body"
        )));

        mockMvc.perform(get("/api/blog/hello"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.slug", is("hello")))
            .andExpect(jsonPath("$.data.readingTime", is(4)));
    }

    @Test
    void returnsNotFoundForAnUnknownOrUnpublishedSlug() throws Exception {
        given(blogService.getBySlug("nope")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/blog/nope"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success", is(false)));
    }
}
