package com.riceerp.backend.auth;

import com.riceerp.backend.otp.OtpService;
import com.riceerp.backend.user.User;
import com.riceerp.backend.user.UserRepository;
import org.springframework.web.bind.annotation.*;
import com.riceerp.backend.security.JwtUtil;


import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final OtpService otpService;
    private final UserRepository userRepository;

    public AuthController(OtpService otpService, UserRepository userRepository) {
        this.otpService = otpService;
        this.userRepository = userRepository;
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
        user = userRepository.save(user);
        isNewUser = true;
    }

    String token = JwtUtil.generateToken(user.getId(), user.getPhoneNumber());

    Map<String, Object> response = new HashMap<>();
    response.put("isNewUser", isNewUser);
    response.put("profileCompleted", user.isProfileCompleted());
    response.put("message", "OTP verified successfully");
    response.put("token", token);


    return response;
    }
}
