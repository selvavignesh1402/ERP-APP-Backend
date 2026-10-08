package com.riceerp.backend.dto;

import com.riceerp.backend.entity.UserProfile;

public record UserProfileResponse(Long userId, String email, String location, String registerNumber, String gstNo) {
    public static UserProfileResponse from(UserProfile profile) {
        return profile == null ? null : new UserProfileResponse(profile.getUserId(), profile.getEmail(),
                profile.getLocation(), profile.getRegisterNumber(), profile.getGstNo());
    }
}
