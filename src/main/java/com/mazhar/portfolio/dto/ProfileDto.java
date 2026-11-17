package com.mazhar.portfolio.dto;

import java.util.List;

/** Field names mirror the Angular side's APP_CONFIG (src/app/core/config/app-config.ts). */
public record ProfileDto(
    String name,
    String role,
    String tagline,
    int yearsOfExperience,
    String careerGoal,
    String currentFocus,
    List<String> futureFocus,
    String availabilityStatus
) {}
