package com.mazhar.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Mirrors the Angular side's Technology (src/app/core/models/technology.model.ts); read by the AI assistant's knowledge base. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TechnologyDto(
    String name,
    String category,
    String level,
    String description,
    String icon,
    boolean featured
) {}
