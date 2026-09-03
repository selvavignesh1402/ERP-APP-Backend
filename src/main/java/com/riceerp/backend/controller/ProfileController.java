package com.riceerp.backend.controller;

import com.riceerp.backend.dto.UpdateProfileRequest;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.entity.UserProfile;
import com.riceerp.backend.exception.NotFoundException;
import com.riceerp.backend.repository.UserProfileRepository;
import com.riceerp.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileController(UserProfileRepository profileRepository,
            UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    // The authenticated user manages their own profile only.
    // The user id always comes from the JWT principal — never from the request path.

    @GetMapping
    public UserProfile getProfile(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return profileRepository.findById(userId)
                .orElse(null);
    }

    @PutMapping
    public Map<String, String> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {

        Long userId = getCurrentUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserProfile profile = profileRepository.findById(userId)
                .orElse(new UserProfile());

        profile.setUserId(userId);
        profile.setEmail(request.getEmail());
        profile.setLocation(request.getLocation());
        profile.setRegisterNumber(request.getRegisterNumber());
        profile.setGstNo(request.getGstNo());

        profileRepository.save(profile);

        user.setProfileCompleted(true);
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Profile completed successfully");
        return response;
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Not authenticated");
        }
        // JwtFilter stores the user id (Long) as the authentication principal
        try {
            return Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Cannot resolve user id from authentication");
        }
    }
}
