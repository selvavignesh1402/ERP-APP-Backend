package com.riceerp.backend.controller;

import com.riceerp.backend.dto.SignupRequest;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.repository.UserRepository;
import com.riceerp.backend.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private OtpService otpService;
    private UserRepository userRepository;
    private com.riceerp.backend.repository.OrganizationMembershipRepository membershipRepository;
    private com.riceerp.backend.repository.OrganizationRepository organizationRepository;
    private PasswordEncoder passwordEncoder;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        otpService = mock(OtpService.class);
        userRepository = mock(UserRepository.class);
        membershipRepository = mock(com.riceerp.backend.repository.OrganizationMembershipRepository.class);
        organizationRepository = mock(com.riceerp.backend.repository.OrganizationRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authController = new AuthController(otpService, userRepository, membershipRepository, organizationRepository, passwordEncoder);
    }

    @Test
    void signupWithPassword_whenPasswordIsNull_shouldThrowException() {
        SignupRequest request = new SignupRequest();
        request.setName("Test User");
        request.setPhoneNumber("1234567890");
        request.setPassword(null);

        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authController.signupWithPassword(request);
        });

        assertEquals("Password must be at least 6 characters long", exception.getMessage());
    }

    @Test
    void signupWithPassword_whenPasswordTooShort_shouldThrowException() {
        SignupRequest request = new SignupRequest();
        request.setName("Test User");
        request.setPhoneNumber("1234567890");
        request.setPassword("123");

        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authController.signupWithPassword(request);
        });

        assertEquals("Password must be at least 6 characters long", exception.getMessage());
    }

    @Test
    void signupWithPassword_whenPasswordIsValid_shouldSucceed() {
        SignupRequest request = new SignupRequest();
        request.setName("Test User");
        request.setPhoneNumber("1234567890");
        request.setPassword("123456");

        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = authController.signupWithPassword(request);

        assertNotNull(result);
        assertEquals("Signup successful", result.get("message"));
        verify(userRepository, times(1)).save(any(User.class));
    }
}
