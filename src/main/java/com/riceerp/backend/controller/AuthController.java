package com.riceerp.backend.controller;

import com.riceerp.backend.dto.LoginRequest;
import com.riceerp.backend.dto.SendOtpRequest;
import com.riceerp.backend.dto.SignupRequest;
import com.riceerp.backend.dto.VerifyOtpRequest;
import com.riceerp.backend.enums.Role;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.repository.UserRepository;
import com.riceerp.backend.security.JwtUtil;
import com.riceerp.backend.service.OtpService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(OtpService otpService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // -------- SEND OTP --------
    @PostMapping("/send-otp")
    public Map<String, String> sendOtp(@RequestBody SendOtpRequest request) {

        otpService.generateAndSaveOtp(request.getPhoneNumber());

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP sent successfully");
        return response;
    }

    // -------- VERIFY OTP --------
    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@RequestBody VerifyOtpRequest request) {

        otpService.verifyOtp(request.getPhoneNumber(), request.getOtp());

        boolean isNewUser;
        User user;

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            user = userRepository.findByPhoneNumber(request.getPhoneNumber()).get();
            isNewUser = false;
        } else {
            user = new User();
            user.setPhoneNumber(request.getPhoneNumber());
            user.setName(request.getName());
            user.setRole(Role.SALES);
            user = userRepository.save(user);
            isNewUser = true;
        }

        String roleName = user.getRole() != null ? user.getRole().name() : Role.SALES.name();
        String token = JwtUtil.generateToken(user.getId(), user.getPhoneNumber(), roleName);

        Map<String, Object> response = new HashMap<>();
        response.put("isNewUser", isNewUser);
        response.put("profileCompleted", user.isProfileCompleted());
        response.put("message", "OTP verified successfully");
        response.put("token", token);
        response.put("role", roleName);

        return response;
    }

    @PostMapping("/signup-password")
    public Map<String, Object> signupWithPassword(@RequestBody SignupRequest request) {

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already registered");
        }

        if (request.getPassword() == null || request.getPassword().trim().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters long");
        }

        User user = new User();
        user.setName(request.getName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setProfileCompleted(false);
        user.setActive(true);
        user.setRole(Role.SALES);

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Signup successful");
        response.put("profileCompleted", false);

        return response;
    }

    @PostMapping("/login-password")
    public Map<String, Object> loginWithPassword(@RequestBody LoginRequest request) {

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account has been deactivated. Contact your administrator.");
        }

        String roleName = user.getRole() != null ? user.getRole().name() : Role.SALES.name();
        String token = JwtUtil.generateToken(user.getId(), user.getPhoneNumber(), roleName);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("profileCompleted", user.isProfileCompleted());
        response.put("userId", user.getId());
        response.put("role", roleName);

        return response;
    }

    // -------- CURRENT USER --------
    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Not authenticated");
        }

        Long userId;
        try {
            userId = Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid authentication principal");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("role", user.getRole() != null ? user.getRole().name() : Role.SALES.name());
        response.put("profileCompleted", user.isProfileCompleted());

        return response;
    }
}
