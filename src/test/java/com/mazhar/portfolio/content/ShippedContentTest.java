package com.mazhar.portfolio.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.mazhar.portfolio.dto.EducationDto;
import com.mazhar.portfolio.dto.ExperienceDto;
import com.mazhar.portfolio.dto.ProfileDto;
import com.mazhar.portfolio.dto.ProjectDto;
import com.mazhar.portfolio.dto.TechnologyDto;

/**
 * Invariants of the content that ships in src/main/resources/content. The Angular pages read
 * several of these fields unguarded (see ApiContractTest), so a file that breaks them would
 * break the site once the API is switched on.
 */
class ShippedContentTest {

    private final ContentLoader loader = new ContentLoader();
    private final List<ProjectDto> projects = loader.loadList("projects.json", ProjectDto.class);
    private final List<ExperienceDto> experiences = loader.loadList("experience.json", ExperienceDto.class);

    @Test
    void projectsHaveUniqueSlugsAndEveryFieldTheProjectsPageReadsUnguarded() {
        assertThat(projects).isNotEmpty();
        assertThat(projects).extracting(ProjectDto::slug).doesNotHaveDuplicates();
        assertThat(projects).allSatisfy(project -> {
            assertThat(project.slug()).matches("[a-z0-9]+(-[a-z0-9]+)*");
            assertThat(project.name()).isNotBlank();
            assertThat(project.shortDescription()).isNotBlank();
            assertThat(project.businessDomain()).isNotBlank();
            assertThat(project.technologyStack()).isNotEmpty();
            assertThat(project.filterTags()).isNotEmpty();
        });
    }

    @Test
    void projectFilterTagsAndCategoriesUseTheValuesTheAngularFiltersKnow() {
        List<String> knownTags = List.of("angular", "enterprise", "dashboard", "ai", "healthcare", "fintech", "other");
        List<String> knownCategories = List.of(
            "enterprise-applications", "fintech", "healthcare", "fleet-management",
            "expense-management", "dashboards", "ai-ocr", "saas", "ui-engineering");

        assertThat(projects).allSatisfy(project -> {
            assertThat(knownTags).containsAll(project.filterTags());
            assertThat(knownCategories).contains(project.category());
        });
    }

    @Test
    void experienceIsNewestFirstWithOneCurrentRoleAndTheCategoriesTheExperiencePageFiltersOn() {
        assertThat(experiences).isNotEmpty();
        assertThat(experiences).extracting(ExperienceDto::startDate).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(experiences).filteredOn(experience -> experience.endDate() == null).hasSize(1);
        assertThat(experiences).allSatisfy(experience -> {
            assertThat(experience.startDate()).matches("\\d{4}-(0[1-9]|1[0-2])");
            assertThat(experience.categories()).isNotEmpty();
            assertThat(experience.responsibilities()).isNotNull();
            assertThat(experience.keyContributions()).isNotNull();
        });
    }

    @Test
    void profileIsCompleteAndClaimsNoMoreExperienceThanTheHistorySupports() {
        ProfileDto profile = loader.loadObject("profile.json", ProfileDto.class);
        assertThat(profile.name()).isNotBlank();
        assertThat(profile.role()).isNotBlank();
        assertThat(profile.futureFocus()).isNotEmpty();

        String earliest = experiences.stream().map(ExperienceDto::startDate).min(String::compareTo).orElseThrow();
        int year = Integer.parseInt(earliest.substring(0, 4));
        int month = Integer.parseInt(earliest.substring(5, 7));
        java.time.LocalDate now = java.time.LocalDate.now();
        int yearsWorked = (now.getYear() - year) - (now.getMonthValue() < month ? 1 : 0);
        assertThat(profile.yearsOfExperience()).isLessThanOrEqualTo(yearsWorked);
    }

    @Test
    void technologiesAreUniqueAndUseTheCategoriesAndLevelsTheSkillsPageKnows() {
        List<TechnologyDto> technologies = loader.loadList("technologies.json", TechnologyDto.class);
        List<String> categories = List.of(
            "frontend-engineering", "state-management", "ui-frameworks", "backend", "ai", "cloud-devops", "development-tools");
        List<String> levels = List.of("primary", "strong", "working-knowledge", "learning");

        assertThat(technologies).isNotEmpty();
        assertThat(technologies).extracting(TechnologyDto::name).doesNotHaveDuplicates();
        assertThat(technologies).allSatisfy(technology -> {
            assertThat(categories).contains(technology.category());
            assertThat(levels).contains(technology.level());
            assertThat(technology.description()).isNotBlank();
        });
    }

    @Test
    void educationIsPresent() {
        List<EducationDto> education = loader.loadList("education.json", EducationDto.class);

        assertThat(education).isNotEmpty();
        assertThat(education).allSatisfy(item -> {
            assertThat(item.degree()).isNotBlank();
            assertThat(item.institution()).isNotBlank();
        });
    }
}
