package com.mazhar.portfolio.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mazhar.portfolio.dto.ApiResponse;
import com.mazhar.portfolio.dto.BlogPostDto;
import com.mazhar.portfolio.service.BlogService;

@RestController
@RequestMapping("/api/blog")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    public ApiResponse<List<BlogPostDto>> getAll() {
        return ApiResponse.success(blogService.getAll());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BlogPostDto>> getBySlug(@PathVariable String slug) {
        return blogService.getBySlug(slug)
            .map(post -> ResponseEntity.ok(ApiResponse.success(post)))
            .orElseGet(() -> ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Article not found: " + slug, List.of())));
    }
}
