package com.mazhar.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Must stay field-for-field compatible with the Angular side's CaseStudy
 * model (src/app/core/models/case-study.model.ts); see ApiContractTest.
 * `relatedProjectSlug` is the only optional field there, so it's omitted when
 * null. Every list is required — the detail page reads
 * `caseStudy.architecture.length` unguarded.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CaseStudyDto(
    String slug,
    String title,
    String summary,
    String relatedProjectSlug,
    String challenge,
    String context,
    String role,
    String approach,
    List<String> architecture,
    String implementation,
    List<String> technicalChallenges,
    String solution,
    List<String> performance,
    String outcome,
    List<String> lessonsLearned
) {}
