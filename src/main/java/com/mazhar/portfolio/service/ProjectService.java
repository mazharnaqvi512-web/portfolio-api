package com.mazhar.portfolio.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.mazhar.portfolio.content.ContentLoader;
import com.mazhar.portfolio.dto.ProjectDto;

/** Serves content/projects.json, generated from the Angular side's projects.data.ts. */
@Service
public class ProjectService {

    private final List<ProjectDto> projects;

    public ProjectService(ContentLoader content) {
        this.projects = content.loadList("projects.json", ProjectDto.class);
    }

    public List<ProjectDto> getAll() {
        return projects;
    }

    public Optional<ProjectDto> getBySlug(String slug) {
        return projects.stream()
            .filter(project -> project.slug().equals(slug))
            .findFirst();
    }
}
