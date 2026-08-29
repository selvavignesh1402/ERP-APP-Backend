package com.riceerp.backend.controller;

import com.riceerp.backend.dto.UpdateUserRequest;
import com.riceerp.backend.dto.UserSummary;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.Role;
import com.riceerp.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserSummary> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        boolean roleChange = request.getRole() != null && request.getRole() != user.getRole();
        boolean activeChange = request.getActive() != null && request.getActive() != user.isActive();

        if (!roleChange && !activeChange) {
            return ResponseEntity.ok(toSummary(user));
        }

        // Last-admin guard: any change away from being an active ADMIN must leave
        // at least one other active ADMIN in the system.
        boolean demoting = roleChange && user.getRole() == Role.ADMIN && request.getRole() != Role.ADMIN;
        boolean deactivating = activeChange && user.isActive() && !request.getActive();
        boolean targetWasEffectiveAdmin = user.getRole() == Role.ADMIN && user.isActive();

        if ((demoting || deactivating) && targetWasEffectiveAdmin) {
            long remainingActiveAdmins = userRepository.countActiveAdmins(Role.ADMIN);
            // remainingActiveAdmins counts this user too if they are currently active+ADMIN;
            // if the change is "demote this user from ADMIN", we check post-change count = (remainingActiveAdmins - 1).
            // if the change is "deactivate this user", same: remainingActiveAdmins - 1.
            if (remainingActiveAdmins <= 1) {
                Map<String, String> err = new HashMap<>();
                err.put("message", "Cannot demote or deactivate the last remaining admin");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
            }
        }

        if (roleChange) {
            user.setRole(request.getRole());
        }
        if (activeChange) {
            user.setActive(request.getActive());
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(toSummary(saved));
    }

    private UserSummary toSummary(User u) {
        UserSummary s = new UserSummary();
        s.setId(u.getId());
        s.setName(u.getName());
        s.setPhoneNumber(u.getPhoneNumber());
        s.setRole(u.getRole() != null ? u.getRole() : Role.SALES);
        s.setActive(u.isActive());
        s.setProfileCompleted(u.isProfileCompleted());
        return s;
    }
}
