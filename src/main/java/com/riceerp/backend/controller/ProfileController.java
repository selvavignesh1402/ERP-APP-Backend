package com.riceerp.backend.controller;

import com.riceerp.backend.dto.UpdateProfileRequest;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.entity.UserProfile;
import com.riceerp.backend.repository.UserProfileRepository;
import com.riceerp.backend.repository.UserRepository;
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

    @GetMapping("/{userId}")
    public UserProfile getProfile(@PathVariable Long userId) {
        return profileRepository.findById(userId)
                .orElse(null);
    }

    @PutMapping("/{userId}")
    public Map<String, String> updateProfile(
            @PathVariable Long userId,
            @RequestBody UpdateProfileRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
}
