package com.riceerp.backend.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.riceerp.backend.dto.FirebaseLoginRequest;
import com.riceerp.backend.dto.LoginRequest;
import com.riceerp.backend.dto.SendOtpRequest;
import com.riceerp.backend.dto.SignupRequest;
import com.riceerp.backend.dto.VerifyOtpRequest;
import com.riceerp.backend.entity.Organization;
import com.riceerp.backend.entity.OrganizationMembership;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.OrgRole;
import com.riceerp.backend.enums.PlatformRole;
import com.riceerp.backend.repository.OrganizationMembershipRepository;
import com.riceerp.backend.repository.OrganizationRepository;
import com.riceerp.backend.repository.UserRepository;
import com.riceerp.backend.security.DefaultPermissionMatrix;
import com.riceerp.backend.security.JwtUtil;
import com.riceerp.backend.service.OtpService;
import com.riceerp.backend.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    public AuthController(OtpService otpService,
                          UserRepository userRepository,
                          OrganizationMembershipRepository membershipRepository,
                          OrganizationRepository organizationRepository,
                          PasswordEncoder passwordEncoder,
                          PermissionService permissionService) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
    }

    // -------- SEND OTP --------
    @PostMapping("/send-otp")
    public Map<String, String> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        otpService.generateAndSaveOtp(request.getPhoneNumber());

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP sent successfully");
        return response;
    }

    private OrganizationMembership getOrCreateDefaultMembership(User user) {
        List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId())
                .stream().filter(OrganizationMembership::isActive).toList();
        if (!memberships.isEmpty()) {
            return memberships.get(0);
        }

        if (user.getPlatformRole() == PlatformRole.MASTER_ADMIN) {
            return null;
        }

        // Auto-provision shop for business owner if they have no active memberships
        Organization org = new Organization();
        String orgName = (user.getName() != null && !user.getName().trim().isEmpty() && !user.getName().startsWith("User "))
                ? user.getName() + "'s Shop"
                : "My Shop";
        org.setName(orgName);
        org = organizationRepository.save(org);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(OrgRole.ADMIN);
        membership.setActive(true);
        membership = membershipRepository.save(membership);

        permissionService.seedPermissionsForOrg(org.getId());
        return membership;
    }

    // -------- VERIFY OTP --------
    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        otpService.verifyOtp(request.getPhoneNumber(), request.getOtp());

        boolean isNewUser;
        User user;

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            user = userRepository.findByPhoneNumber(request.getPhoneNumber()).get();
            isNewUser = false;
        } else {
            user = new User();
            user.setPhoneNumber(request.getPhoneNumber());
            String name = request.getName();
            if (name == null || name.trim().isEmpty()) {
                String phone = request.getPhoneNumber();
                name = "User " + (phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone);
            }
            user.setName(name);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setPlatformRole(PlatformRole.USER);
            user.setActive(true);
            user.setProfileCompleted(false);
            user = userRepository.save(user);
            isNewUser = true;
        }

        Long orgId = null;
        OrgRole orgRole = null;
        String roleName = user.getPlatformRole() != null ? user.getPlatformRole().name() : PlatformRole.USER.name();

        if (user.getPlatformRole() == PlatformRole.MASTER_ADMIN) {
            roleName = PlatformRole.MASTER_ADMIN.name();
            List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId())
                    .stream().filter(OrganizationMembership::isActive).toList();
            if (!memberships.isEmpty()) {
                orgId = memberships.get(0).getOrganization().getId();
                orgRole = memberships.get(0).getRole();
            }
        } else {
            OrganizationMembership membership = getOrCreateDefaultMembership(user);
            if (membership != null) {
                orgId = membership.getOrganization().getId();
                orgRole = membership.getRole();
                roleName = orgRole.name();
            }
        }

        String token = JwtUtil.generateToken(user.getId(), user.getPhoneNumber(), user.getPlatformRole(), orgId, orgRole);

        Set<String> permissions;
        if (user.getPlatformRole() == PlatformRole.MASTER_ADMIN) {
            permissions = new HashSet<>(DefaultPermissionMatrix.ALL_PERMISSIONS);
        } else if (orgId != null && orgRole != null) {
            permissions = permissionService.getEffectivePermissions(orgId, orgRole);
        } else {
            permissions = Collections.emptySet();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("isNewUser", isNewUser);
        response.put("profileCompleted", user.isProfileCompleted());
        response.put("message", "OTP verified successfully");
        response.put("token", token);
        response.put("role", roleName);
        response.put("orgRole", orgRole != null ? orgRole.name() : null);
        response.put("organizationId", orgId);
        response.put("permissions", permissions);

        return response;
    }

    // -------- FIREBASE PHONE LOGIN --------
    @PostMapping("/firebase-login")
    public Map<String, Object> firebaseLogin(@Valid @RequestBody FirebaseLoginRequest request) {
        FirebaseToken decodedToken;
        try {
            decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getToken());
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Invalid or expired Firebase token: " + e.getMessage());
        }

        String phoneNumber = (String) decodedToken.getClaims().get("phone_number");
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new RuntimeException("Phone number not found in Firebase token");
        }

        boolean isNewUser;
        User user;

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            user = userRepository.findByPhoneNumber(phoneNumber).get();
            isNewUser = false;
        } else {
            user = new User();
            user.setPhoneNumber(phoneNumber);
            String name = request.getName();
            if (name == null || name.trim().isEmpty()) {
                name = "User " + (phoneNumber.length() >= 4 ? phoneNumber.substring(phoneNumber.length() - 4) : phoneNumber);
            }
            user.setName(name);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setPlatformRole(PlatformRole.USER);
            user.setActive(true);
            user.setProfileCompleted(false);
            user = userRepository.save(user);
            isNewUser = true;
        }

        Long orgId = null;
        OrgRole orgRole = null;
        String roleName = user.getPlatformRole() != null ? user.getPlatformRole().name() : PlatformRole.USER.name();

        if (user.getPlatformRole() == PlatformRole.MASTER_ADMIN) {
            roleName = PlatformRole.MASTER_ADMIN.name();
            List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId())
                    .stream().filter(OrganizationMembership::isActive).toList();
            if (!memberships.isEmpty()) {
                orgId = memberships.get(0).getOrganization().getId();
                orgRole = memberships.get(0).getRole();
            }
        } else {
            OrganizationMembership membership = getOrCreateDefaultMembership(user);
            if (membership != null) {
                orgId = membership.getOrganization().getId();
                orgRole = membership.getRole();
                roleName = orgRole.name();
            }
        }

        String token = JwtUtil.generateToken(user.getId(), user.getPhoneNumber(), user.getPlatformRole(), orgId, orgRole);

        Set<String> permissions;
        if (user.getPlatformRole() == PlatformRole.MASTER_ADMIN) {
            permissions = new HashSet<>(DefaultPermissionMatrix.ALL_PERMISSIONS);
        } else if (orgId != null && orgRole != null) {
            permissions = permissionService.getEffectivePermissions(orgId, orgRole);
        } else {
            permissions = Collections.emptySet();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("isNewUser", isNewUser);
        response.put("profileCompleted", user.isProfileCompleted());
        response.put("message", "Firebase login successful");
        response.put("token", token);
        response.put("role", roleName);
        response.put("orgRole", orgRole != null ? orgRole.name() : null);
        response.put("organizationId", orgId);
        response.put("permissions", permissions);
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("name", user.getName());

        return response;
    }

    @PostMapping("/signup-password")
    public Map<String, Object> signupWithPassword(@Valid @RequestBody SignupRequest request) {
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
        user.setPlatformRole(PlatformRole.USER);
        user = userRepository.save(user);

        // Provision a new Organization for the new business owner
        Organization org = new Organization();
        String orgName = (request.getName() != null && !request.getName().trim().isEmpty())
                ? request.getName() + "'s Shop"
                : "My Shop";
        org.setName(orgName);
        org = organizationRepository.save(org);

        // Assign user as ADMIN of their new Organization
        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(OrgRole.ADMIN);
        membership.setActive(true);
        membershipRepository.save(membership);

        // Seed permission matrix for the new shop
        permissionService.seedPermissionsForOrg(org.getId());

        String token = JwtUtil.generateToken(user.getId(), user.getPhoneNumber(), user.getPlatformRole(), org.getId(), OrgRole.ADMIN);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Signup successful");
        response.put("profileCompleted", false);
        response.put("token", token);
        response.put("organizationId", org.getId());
        response.put("orgRole", OrgRole.ADMIN.name());

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

        Long orgId = null;
        OrgRole orgRole = null;
        String roleName = user.getPlatformRole() != null ? user.getPlatformRole().name() : PlatformRole.USER.name();

        if (user.getPlatformRole() == PlatformRole.MASTER_ADMIN) {
            roleName = PlatformRole.MASTER_ADMIN.name();
            List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId())
                    .stream().filter(OrganizationMembership::isActive).toList();
            if (!memberships.isEmpty()) {
                orgId = memberships.get(0).getOrganization().getId();
                orgRole = memberships.get(0).getRole();
            }
        } else {
            OrganizationMembership membership = getOrCreateDefaultMembership(user);
            if (membership != null) {
                orgId = membership.getOrganization().getId();
                orgRole = membership.getRole();
                roleName = orgRole.name();
            }
        }

        String token = JwtUtil.generateToken(user.getId(), user.getPhoneNumber(), user.getPlatformRole(), orgId, orgRole);

        Set<String> permissions;
        if (user.getPlatformRole() == PlatformRole.MASTER_ADMIN) {
            permissions = new HashSet<>(DefaultPermissionMatrix.ALL_PERMISSIONS);
        } else if (orgId != null && orgRole != null) {
            permissions = permissionService.getEffectivePermissions(orgId, orgRole);
        } else {
            permissions = Collections.emptySet();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("profileCompleted", user.isProfileCompleted());
        response.put("userId", user.getId());
        response.put("role", roleName);
        response.put("orgRole", orgRole != null ? orgRole.name() : null);
        response.put("organizationId", orgId);
        response.put("permissions", permissions);

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

        Long orgId = null;
        OrgRole orgRole = null;
        String roleName = user.getPlatformRole() != null ? user.getPlatformRole().name() : PlatformRole.USER.name();

        if (user.getPlatformRole() == PlatformRole.MASTER_ADMIN) {
            roleName = PlatformRole.MASTER_ADMIN.name();
            List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId())
                    .stream().filter(OrganizationMembership::isActive).toList();
            if (!memberships.isEmpty()) {
                orgId = memberships.get(0).getOrganization().getId();
                orgRole = memberships.get(0).getRole();
            }
        } else {
            OrganizationMembership membership = getOrCreateDefaultMembership(user);
            if (membership != null) {
                orgId = membership.getOrganization().getId();
                orgRole = membership.getRole();
                roleName = orgRole.name();
            }
        }

        Set<String> permissions;
        if (user.getPlatformRole() == PlatformRole.MASTER_ADMIN) {
            permissions = new HashSet<>(DefaultPermissionMatrix.ALL_PERMISSIONS);
        } else if (orgId != null && orgRole != null) {
            permissions = permissionService.getEffectivePermissions(orgId, orgRole);
        } else {
            permissions = Collections.emptySet();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("role", roleName);
        response.put("orgRole", orgRole != null ? orgRole.name() : null);
        response.put("organizationId", orgId);
        response.put("permissions", permissions);
        response.put("profileCompleted", user.isProfileCompleted());

        return response;
    }
}
