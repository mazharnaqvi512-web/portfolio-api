package com.mazhar.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Must stay field-for-field compatible with the Angular side's Experience
 * model (src/app/core/models/experience.model.ts). Optional (`?`) fields are
 * omitted when null; `endDate` is the exception — the Angular model types it
 * `string | null` (null means "current role"), so it's always sent, even
 * when null. `categories` is required there: the Experience page filters on
 * `experience.categories.includes(...)` unguarded.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExperienceDto(
    String company,
    String role,
    String employmentType,
    String location,
    String startDate,
    @JsonInclude(JsonInclude.Include.ALWAYS) String endDate,
    String description,
    List<String> responsibilities,
    List<String> technologies,
    List<String> keyContributions,
    List<String> categories,
    String companyWebsite,
    List<ExperienceLinkDto> projectLinks
) {

    /** Mirrors the Angular side's ExperienceLink. */
    public record ExperienceLinkDto(String label, String url) {}
}
