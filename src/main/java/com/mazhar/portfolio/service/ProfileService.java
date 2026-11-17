package com.mazhar.portfolio.service;

import org.springframework.stereotype.Service;
import com.mazhar.portfolio.content.ContentLoader;
import com.mazhar.portfolio.dto.ProfileDto;

/** Serves content/profile.json, generated from the Angular side's APP_CONFIG. */
@Service
public class ProfileService {

    private final ProfileDto profile;

    public ProfileService(ContentLoader content) {
        this.profile = content.loadObject("profile.json", ProfileDto.class);
    }

    public ProfileDto getProfile() {
        return profile;
    }
}
