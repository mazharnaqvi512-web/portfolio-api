package com.mazhar.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Must stay field-for-field compatible with the Angular side's Project model
 * (src/app/core/models/project.model.ts). Fields the Angular model marks
 * optional (`?`) are omitted from the JSON when null, rather than sent as
 * `null` — which is what a TypeScript `T | undefined` actually expects.
 * `filterTags` is required there: the Projects page calls
 * `project.filterTags.includes(...)` unguarded, so it must never be missing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectDto(
    String name,
    String slug,
    String shortDescription,
    String category,
    List<String> technologyStack,
    String role,
    String projectType,
    String businessDomain,
    String thumbnail,
    boolean featured,
    List<String> filterTags,
    String liveDemoUrl,
    String githubUrl,
    String caseStudySlug,
    String videoUrl,
    String overview,
    String problem,
    String solution,
    List<String> keyFeatures,
    List<String> architecture,
    List<String> technicalChallenges,
    List<String> performanceNotes,
    List<String> results,
    List<String> lessonsLearned,
    List<ProjectScreenshotDto> screenshots
) {

    /** Mirrors the Angular side's ProjectScreenshot. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProjectScreenshotDto(String url, String alt, String caption) {}
}
