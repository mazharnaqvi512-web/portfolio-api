package com.mazhar.portfolio.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mazhar.portfolio.dto.ApiResponse;
import com.mazhar.portfolio.dto.ProfileDto;
import com.mazhar.portfolio.service.ProfileService;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<ProfileDto> getProfile() {
        return ApiResponse.success(profileService.getProfile());
    }
}
