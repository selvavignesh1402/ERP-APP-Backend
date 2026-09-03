package com.riceerp.backend.controller;

import com.riceerp.backend.entity.Organization;
import com.riceerp.backend.entity.OrganizationMembership;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.OrgRole;
import com.riceerp.backend.enums.PlatformRole;
import com.riceerp.backend.repository.OrganizationMembershipRepository;
import com.riceerp.backend.repository.OrganizationRepository;
import com.riceerp.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/master-admin")
public class MasterAdminController {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    public MasterAdminController(OrganizationRepository organizationRepository,
                                 UserRepository userRepository,
                                 OrganizationMembershipRepository membershipRepository,
                                 PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. Platform Metrics Summary
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPlatformSummary() {
        long totalOrgs = organizationRepository.count();
        long totalUsers = userRepository.count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrganizations", totalOrgs);
        summary.put("totalUsers", totalUsers);
        summary.put("activeOrganizations", totalOrgs);
        summary.put("platformHealth", "OPERATIONAL");
        summary.put("systemVersion", "v2.0-SaaS");

        return ResponseEntity.ok(summary);
    }

    // 2. All Organizations Directory with User Counts
    @GetMapping("/organizations")
    public ResponseEntity<List<Map<String, Object>>> getAllOrganizations() {
        List<Organization> orgs = organizationRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Organization org : orgs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", org.getId());
            item.put("name", org.getName());
            item.put("createdAt", org.getCreatedAt());
            item.put("status", "ACTIVE");
            
            long memberCount = membershipRepository.findAll().stream()
                    .filter(m -> m.getOrganization().getId().equals(org.getId()))
                    .count();
            item.put("userCount", memberCount);

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    // 3. Platform Users Directory
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (User u : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getName());
            item.put("phoneNumber", u.getPhoneNumber());
            item.put("platformRole", u.getPlatformRole() != null ? u.getPlatformRole().name() : "USER");
            item.put("profileCompleted", u.isProfileCompleted());
            item.put("isActive", u.isActive());

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    // 4. Create New Organization with Admin User & Membership
    @PostMapping("/organizations")
    @Transactional
    public ResponseEntity<Map<String, Object>> createOrganization(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Shop / Organization name is required"));
        }

        String adminPhone = body.get("adminPhone");
        String adminName = body.get("adminName");
        String adminPassword = body.get("adminPassword");

        if (adminPhone == null || adminPhone.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Admin phone number is required to assign shop owner"));
        }

        if (adminName == null || adminName.trim().isEmpty()) {
            adminName = name.trim() + " Admin";
        }
        if (adminPassword == null || adminPassword.trim().isEmpty()) {
            adminPassword = "admin123";
        }

        // 1. Create and save Organization
        Organization org = new Organization();
        org.setName(name.trim());
        org.setCreatedAt(LocalDateTime.now());
        Organization savedOrg = organizationRepository.save(org);

        // 2. Find or create User
        final String finalAdminName = adminName.trim();
        final String finalAdminPassword = adminPassword;
        User adminUser = userRepository.findByPhoneNumber(adminPhone.trim())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setName(finalAdminName);
                    newUser.setPhoneNumber(adminPhone.trim());
                    newUser.setPasswordHash(passwordEncoder.encode(finalAdminPassword));
                    newUser.setPlatformRole(PlatformRole.USER);
                    newUser.setProfileCompleted(false);
                    newUser.setActive(true);
                    newUser.setCreatedAt(LocalDateTime.now());
                    return userRepository.save(newUser);
                });

        // 3. Create and save OrganizationMembership
        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(adminUser);
        membership.setOrganization(savedOrg);
        membership.setRole(OrgRole.ADMIN);
        membership.setJoinedAt(LocalDateTime.now());
        membershipRepository.save(membership);

        Map<String, Object> result = new HashMap<>();
        result.put("id", savedOrg.getId());
        result.put("name", savedOrg.getName());
        result.put("createdAt", savedOrg.getCreatedAt());
        result.put("status", "ACTIVE");
        result.put("userCount", 1);
        result.put("adminUserId", adminUser.getId());
        result.put("adminName", adminUser.getName());
        result.put("adminPhone", adminUser.getPhoneNumber());

        return ResponseEntity.ok(result);
    }
}

