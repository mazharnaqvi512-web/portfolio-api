package com.mazhar.portfolio.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mazhar.portfolio.dto.ApiResponse;
import com.mazhar.portfolio.dto.CaseStudyDto;
import com.mazhar.portfolio.service.CaseStudyService;

@RestController
@RequestMapping("/api/case-studies")
public class CaseStudyController {

    private final CaseStudyService caseStudyService;

    public CaseStudyController(CaseStudyService caseStudyService) {
        this.caseStudyService = caseStudyService;
    }

    @GetMapping
    public ApiResponse<List<CaseStudyDto>> getAll() {
        return ApiResponse.success(caseStudyService.getAll());
    }
}
