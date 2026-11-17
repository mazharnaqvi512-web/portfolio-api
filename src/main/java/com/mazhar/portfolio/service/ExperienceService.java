package com.mazhar.portfolio.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.mazhar.portfolio.content.ContentLoader;
import com.mazhar.portfolio.dto.ExperienceDto;

/** Serves content/experience.json, generated from the Angular side's experience.data.ts. */
@Service
public class ExperienceService {

    private final List<ExperienceDto> experiences;

    public ExperienceService(ContentLoader content) {
        this.experiences = content.loadList("experience.json", ExperienceDto.class);
    }

    public List<ExperienceDto> getAll() {
        return experiences;
    }
}
