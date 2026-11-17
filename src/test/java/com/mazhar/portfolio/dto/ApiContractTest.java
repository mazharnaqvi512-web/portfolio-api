package com.mazhar.portfolio.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins the JSON these DTOs produce to what the Angular models require
 * (src/app/core/models/project.model.ts and experience.model.ts). The Angular
 * pages call things like `project.filterTags.includes(...)` unguarded, so a
 * required key silently going missing here breaks the site only once real
 * data exists — this fails the build instead.
 */
class ApiContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> REQUIRED_PROJECT_KEYS = List.of(
        "name", "slug", "shortDescription", "category", "technologyStack",
        "role", "projectType", "businessDomain", "featured", "filterTags"
    );

    private static final List<String> REQUIRED_EXPERIENCE_KEYS = List.of(
        "company", "role", "employmentType", "location", "startDate", "endDate",
        "description", "responsibilities", "technologies", "keyContributions", "categories"
    );

    private ProjectDto minimalProject() {
        return new ProjectDto(
            "Fleet Tracker", "fleet-tracker", "Tracks fleets.", "fleet-management", List.of("Angular"),
            "Lead Developer", "Web App", "Logistics", null, true, List.of("angular", "enterprise"),
            null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    @Test
    void projectJsonContainsEveryKeyTheAngularModelRequires() throws Exception {
        JsonNode json = objectMapper.valueToTree(minimalProject());

        for (String key : REQUIRED_PROJECT_KEYS) {
            assertThat(json.has(key)).as("required Project key '%s'", key).isTrue();
        }
        assertThat(json.get("filterTags").isArray()).isTrue();
    }

    @Test
    void projectJsonOmitsUnsetOptionalFieldsInsteadOfSendingNull() {
        JsonNode json = objectMapper.valueToTree(minimalProject());

        for (String optionalKey : List.of("thumbnail", "liveDemoUrl", "githubUrl", "caseStudySlug", "videoUrl",
                "overview", "problem", "solution", "keyFeatures", "architecture", "technicalChallenges",
                "performanceNotes", "results", "lessonsLearned", "screenshots")) {
            assertThat(json.has(optionalKey)).as("optional Project key '%s' should be omitted when unset", optionalKey).isFalse();
        }
    }

    @Test
    void projectScreenshotsSerializeWithTheAngularFieldNames() {
        ProjectDto project = new ProjectDto(
            "P", "p", "d", "saas", List.of(), "r", "t", "b", null, false, List.of(),
            null, null, null, null, null, null, null, null, null, null, null, null, null,
            List.of(new ProjectDto.ProjectScreenshotDto("/a.png", "Alt text", null))
        );

        JsonNode screenshot = objectMapper.valueToTree(project).get("screenshots").get(0);

        assertThat(screenshot.get("url").asText()).isEqualTo("/a.png");
        assertThat(screenshot.get("alt").asText()).isEqualTo("Alt text");
        assertThat(screenshot.has("caption")).isFalse();
    }

    @Test
    void experienceJsonContainsEveryKeyTheAngularModelRequires() {
        ExperienceDto experience = new ExperienceDto(
            "Acme", "Senior Angular Developer", "full-time", "Remote", "2021-01", null, "Built things.",
            List.of("Led a team"), List.of("Angular"), List.of("Cut load time"), List.of("angular", "enterprise"),
            null, null
        );

        JsonNode json = objectMapper.valueToTree(experience);

        for (String key : REQUIRED_EXPERIENCE_KEYS) {
            assertThat(json.has(key)).as("required Experience key '%s'", key).isTrue();
        }
    }

    @Test
    void experienceSendsEndDateAsExplicitNullForACurrentRole() {
        ExperienceDto current = new ExperienceDto(
            "Acme", "Dev", "full-time", "Remote", "2021-01", null, "d",
            List.of(), List.of(), List.of(), List.of(), null, null
        );

        JsonNode json = objectMapper.valueToTree(current);

        assertThat(json.has("endDate")).isTrue();
        assertThat(json.get("endDate").isNull()).isTrue();
        assertThat(json.has("companyWebsite")).isFalse();
        assertThat(json.has("projectLinks")).isFalse();
    }

    // Required keys of src/app/core/models/case-study.model.ts and blog-post.model.ts.
    private static final List<String> REQUIRED_CASE_STUDY_KEYS = List.of(
        "slug", "title", "summary", "challenge", "context", "role", "approach", "architecture",
        "implementation", "technicalChallenges", "solution", "performance", "outcome", "lessonsLearned"
    );

    private static final List<String> REQUIRED_BLOG_POST_KEYS = List.of(
        "id", "slug", "title", "excerpt", "category", "tags", "publishedAt", "readingTime",
        "author", "featured", "status", "content"
    );

    @Test
    void caseStudyJsonContainsEveryKeyTheAngularModelRequires() {
        CaseStudyDto caseStudy = new CaseStudyDto(
            "migration", "A migration", "Summary", null, "Challenge", "Context", "Lead", "Approach",
            List.of("Frontend", "API"), "Implementation", List.of("Hard bit"), "Solution",
            List.of("Fast"), "Outcome", List.of("Learned")
        );

        JsonNode json = objectMapper.valueToTree(caseStudy);

        for (String key : REQUIRED_CASE_STUDY_KEYS) {
            assertThat(json.has(key)).as("required CaseStudy key '%s'", key).isTrue();
        }
        assertThat(json.get("architecture").isArray()).isTrue();
        assertThat(json.has("relatedProjectSlug")).as("optional key omitted when unset").isFalse();
    }

    @Test
    void blogPostJsonContainsEveryKeyTheAngularModelRequires() {
        BlogPostDto post = new BlogPostDto(
            "1", "hello", "Hello", "Excerpt", "angular", List.of("angular"), "2026-01-01",
            null, 4, "Syed Mazhar Hussain", null, false, "published", "Body"
        );

        JsonNode json = objectMapper.valueToTree(post);

        for (String key : REQUIRED_BLOG_POST_KEYS) {
            assertThat(json.has(key)).as("required BlogPost key '%s'", key).isTrue();
        }
        assertThat(json.get("tags").isArray()).isTrue();
        assertThat(json.get("readingTime").isNumber()).isTrue();
        assertThat(json.has("updatedAt")).as("optional key omitted when unset").isFalse();
        assertThat(json.has("coverImage")).as("optional key omitted when unset").isFalse();
    }
}
