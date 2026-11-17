package com.mazhar.portfolio.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.mazhar.portfolio.content.ContentLoader;
import com.mazhar.portfolio.dto.CaseStudyDto;

/** Serves content/case-studies.json, generated from the Angular side's case-studies.data.ts (empty until the first case study is written). */
@Service
public class CaseStudyService {

    private final List<CaseStudyDto> caseStudies;

    public CaseStudyService(ContentLoader content) {
        this.caseStudies = content.loadList("case-studies.json", CaseStudyDto.class);
    }

    public List<CaseStudyDto> getAll() {
        return caseStudies;
    }
}
