package com.riceerp.backend.config;

import com.riceerp.backend.enums.Role;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Init Default Admin User
        String adminPhone = "9999999999";
        if (!userRepository.existsByPhoneNumber(adminPhone)) {
            User admin = new User();
            admin.setName("Default Admin");
            admin.setPhoneNumber(adminPhone);
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setProfileCompleted(true);
            admin.setActive(true);
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);
            System.out.println("Default Admin User created with Phone: " + adminPhone + " and Password: admin123");
        }
    }
}
