package com.mazhar.portfolio.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mazhar.portfolio.dto.ApiResponse;
import com.mazhar.portfolio.dto.ProjectDto;
import com.mazhar.portfolio.service.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<List<ProjectDto>> getAll() {
        return ApiResponse.success(projectService.getAll());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProjectDto>> getBySlug(@PathVariable String slug) {
        return projectService.getBySlug(slug)
            .map(project -> ResponseEntity.ok(ApiResponse.success(project)))
            .orElseGet(() -> ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Project not found: " + slug, List.of())));
    }
}
