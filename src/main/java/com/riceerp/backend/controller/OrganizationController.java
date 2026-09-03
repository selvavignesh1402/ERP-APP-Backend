package com.riceerp.backend.controller;

import com.riceerp.backend.entity.Organization;
import com.riceerp.backend.entity.OrganizationInvite;
import com.riceerp.backend.entity.OrganizationMembership;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.OrgRole;
import com.riceerp.backend.repository.OrganizationMembershipRepository;
import com.riceerp.backend.repository.OrganizationRepository;
import com.riceerp.backend.repository.UserRepository;
import com.riceerp.backend.security.JwtUtil;
import com.riceerp.backend.service.InviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final InviteService inviteService;
    private final UserRepository userRepository;

    public OrganizationController(OrganizationMembershipRepository membershipRepository,
                                  OrganizationRepository organizationRepository,
                                  InviteService inviteService,
                                  UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.inviteService = inviteService;
        this.userRepository = userRepository;
    }

    // List all organizations the logged-in user is part of
    @GetMapping("/my")
    public List<Map<String, Object>> getMyOrganizations(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        List<OrganizationMembership> memberships = membershipRepository.findByUserId(userId);
        
        return memberships.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("organizationId", m.getOrganization().getId());
            map.put("name", m.getOrganization().getName());
            map.put("role", m.getRole().name());
            return map;
        }).collect(Collectors.toList());
    }

    // Create a new Organization
    @PostMapping("")
    public ResponseEntity<?> createOrganization(@RequestBody Map<String, String> request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        User user = userRepository.findById(userId).orElseThrow();
        
        Organization org = new Organization();
        org.setName(request.get("name"));
        // Additional org settings could go here
        
        org = organizationRepository.save(org);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(OrgRole.ADMIN); // The creator is the ADMIN
        
        membershipRepository.save(membership);
        
        String roleName = user.getPlatformRole() != null ? user.getPlatformRole().name() : "USER";
        String token = JwtUtil.generateToken(userId, user.getPhoneNumber(), roleName, org.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Organization created successfully");
        response.put("organizationId", org.getId());
        response.put("token", token);
        response.put("orgRole", OrgRole.ADMIN.name());
        
        return ResponseEntity.ok(response);
    }
    
    // Switch tenant: Generate a new token scoped to the selected organization
    @PostMapping("/select")
    public ResponseEntity<?> selectOrganization(@RequestParam Long organizationId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        
        // Verify user is a member
        OrganizationMembership membership = membershipRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new RuntimeException("User is not a member of this organization"));

        User user = userRepository.findById(userId).orElseThrow();
        String roleName = user.getPlatformRole() != null ? user.getPlatformRole().name() : "USER";
        
        // Generate new token with organizationId
        String token = JwtUtil.generateToken(userId, user.getPhoneNumber(), roleName, organizationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("organizationId", organizationId);
        response.put("orgRole", membership.getRole().name());
        
        return ResponseEntity.ok(response);
    }

    // ─── TEAM & STAFF MANAGEMENT ───

    // List all team members in current organization
    @GetMapping("/members")
    public List<Map<String, Object>> getOrganizationMembers(Authentication authentication) {
        Long orgId = com.riceerp.backend.security.TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No active organization context found");
        }

        List<OrganizationMembership> members = membershipRepository.findByOrganizationId(orgId);
        return members.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("membershipId", m.getId());
            map.put("userId", m.getUser().getId());
            map.put("name", m.getUser().getName());
            map.put("phoneNumber", m.getUser().getPhoneNumber());
            map.put("role", m.getRole().name());
            map.put("isActive", m.isActive());
            map.put("joinedAt", m.getJoinedAt());
            return map;
        }).collect(Collectors.toList());
    }

    // Direct Staff Creation / Provisioning by Admin
    @PostMapping("/staff")
    public ResponseEntity<?> createStaffDirectly(@RequestBody Map<String, String> request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        Long orgId = com.riceerp.backend.security.TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No active organization context found");
        }

        OrganizationMembership adminMembership = membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new RuntimeException("User not in organization"));
        if (adminMembership.getRole() != OrgRole.ADMIN) {
            throw new RuntimeException("Only shop Admins can add staff members directly");
        }

        String name = request.get("name");
        String phone = request.get("phoneNumber");
        String password = request.get("password");
        String roleStr = request.get("role");

        if (name == null || phone == null || roleStr == null) {
            throw new RuntimeException("Name, phone number, and role are required");
        }

        OrgRole role;
        try {
            role = OrgRole.valueOf(roleStr.toUpperCase());
        } catch (Exception e) {
            role = OrgRole.SALES;
        }

        // Find or create User
        User user = userRepository.findByPhoneNumber(phone.trim()).orElse(null);
        if (user == null) {
            user = new User();
            user.setName(name.trim());
            user.setPhoneNumber(phone.trim());
            String rawPassword = (password != null && !password.trim().isEmpty()) ? password.trim() : "staff123";
            user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(rawPassword));
            user.setPlatformRole(com.riceerp.backend.enums.PlatformRole.USER);
            user = userRepository.save(user);
        } else {
            // Check if already in this org
            Optional<OrganizationMembership> existing = membershipRepository.findByUserIdAndOrganizationId(user.getId(), orgId);
            if (existing.isPresent()) {
                throw new RuntimeException("This staff member is already part of the shop!");
            }
        }

        Organization org = adminMembership.getOrganization();
        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(role);
        membership.setActive(true);
        membership = membershipRepository.save(membership);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Staff member created successfully");
        response.put("membershipId", membership.getId());
        response.put("userId", user.getId());
        response.put("name", user.getName());
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("role", membership.getRole().name());
        response.put("isActive", membership.isActive());

        return ResponseEntity.ok(response);
    }

    // Update Staff Role
    @PutMapping("/members/{membershipId}/role")
    public ResponseEntity<?> updateMemberRole(@PathVariable Long membershipId, @RequestBody Map<String, String> request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        Long orgId = com.riceerp.backend.security.TenantContext.getCurrentTenant();

        OrganizationMembership adminMembership = membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new RuntimeException("User not in organization"));
        if (adminMembership.getRole() != OrgRole.ADMIN) {
            throw new RuntimeException("Only shop Admins can modify staff roles");
        }

        OrganizationMembership target = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!target.getOrganization().getId().equals(orgId)) {
            throw new RuntimeException("Unauthorized cross-tenant operation");
        }

        String newRoleStr = request.get("role");
        target.setRole(OrgRole.valueOf(newRoleStr.toUpperCase()));
        membershipRepository.save(target);

        return ResponseEntity.ok(Map.of("message", "Staff role updated to " + target.getRole().name()));
    }

    // Toggle Staff Active / Deactivated Status
    @PutMapping("/members/{membershipId}/status")
    public ResponseEntity<?> toggleMemberStatus(@PathVariable Long membershipId, @RequestBody Map<String, Boolean> request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        Long orgId = com.riceerp.backend.security.TenantContext.getCurrentTenant();

        OrganizationMembership adminMembership = membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new RuntimeException("User not in organization"));
        if (adminMembership.getRole() != OrgRole.ADMIN) {
            throw new RuntimeException("Only shop Admins can activate/deactivate staff");
        }

        OrganizationMembership target = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!target.getOrganization().getId().equals(orgId)) {
            throw new RuntimeException("Unauthorized cross-tenant operation");
        }

        Boolean active = request.get("isActive");
        target.setActive(active != null ? active : !target.isActive());
        membershipRepository.save(target);

        return ResponseEntity.ok(Map.of("message", "Staff account status updated", "isActive", target.isActive()));
    }

    // Remove Staff Member from Organization
    @DeleteMapping("/members/{membershipId}")
    public ResponseEntity<?> removeMember(@PathVariable Long membershipId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        Long orgId = com.riceerp.backend.security.TenantContext.getCurrentTenant();

        OrganizationMembership adminMembership = membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new RuntimeException("User not in organization"));
        if (adminMembership.getRole() != OrgRole.ADMIN) {
            throw new RuntimeException("Only shop Admins can remove staff");
        }

        OrganizationMembership target = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!target.getOrganization().getId().equals(orgId)) {
            throw new RuntimeException("Unauthorized cross-tenant operation");
        }

        if (target.getUser().getId().equals(userId)) {
            throw new RuntimeException("Shop Admin cannot remove their own account");
        }

        membershipRepository.delete(target);
        return ResponseEntity.ok(Map.of("message", "Staff member removed successfully"));
    }

    // Invite a new staff member
    @PostMapping("/invite")
    public ResponseEntity<?> inviteStaff(@RequestBody Map<String, String> request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        Long orgId = com.riceerp.backend.security.TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No organization context found");
        }
        
        OrganizationMembership inviterMembership = membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new RuntimeException("User not in organization"));
                
        if (inviterMembership.getRole() != OrgRole.ADMIN) {
            throw new RuntimeException("Only admins can invite staff");
        }

        String phone = request.get("phoneNumber");
        OrgRole role = OrgRole.valueOf(request.get("role").toUpperCase());
        User inviter = userRepository.findById(userId).orElseThrow();
        
        OrganizationInvite invite = inviteService.createInvite(inviterMembership.getOrganization(), inviter, phone, role);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Invite created successfully");
        response.put("token", invite.getToken());
        response.put("inviteePhoneNumber", phone);
        String inviteLink = "https://riceerp.com/invite?token=" + invite.getToken();
        response.put("inviteLink", inviteLink);
        
        return ResponseEntity.ok(response);
    }

    // List Pending Invites
    @GetMapping("/invites")
    public List<Map<String, Object>> getPendingInvites() {
        Long orgId = com.riceerp.backend.security.TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No active organization context found");
        }

        return inviteService.getInvitesForOrg(orgId).stream().map(inv -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", inv.getId());
            map.put("token", inv.getToken());
            map.put("inviteePhoneNumber", inv.getInviteePhoneNumber());
            map.put("role", inv.getRole().name());
            map.put("status", inv.getStatus());
            map.put("createdAt", inv.getCreatedAt());
            map.put("expiresAt", inv.getExpiresAt());
            map.put("inviteLink", "https://riceerp.com/invite?token=" + inv.getToken());
            return map;
        }).collect(Collectors.toList());
    }

    // Cancel / Delete Invite
    @DeleteMapping("/invites/{inviteId}")
    public ResponseEntity<?> cancelInvite(@PathVariable Long inviteId, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        Long orgId = com.riceerp.backend.security.TenantContext.getCurrentTenant();

        OrganizationMembership adminMembership = membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new RuntimeException("User not in organization"));
        if (adminMembership.getRole() != OrgRole.ADMIN) {
            throw new RuntimeException("Only admins can cancel invites");
        }

        inviteService.cancelInvite(inviteId, orgId);
        return ResponseEntity.ok(Map.of("message", "Invite cancelled successfully"));
    }
    
    // Get invite details
    @GetMapping("/invite-details")
    public ResponseEntity<?> getInviteDetails(@RequestParam String token) {
        OrganizationInvite invite = inviteService.getInvite(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("organizationName", invite.getOrganization().getName());
        response.put("role", invite.getRole().name());
        response.put("invitedBy", invite.getInvitedBy().getName());
        response.put("inviteePhoneNumber", invite.getInviteePhoneNumber());
        response.put("status", invite.getStatus());
        
        return ResponseEntity.ok(response);
    }
    
    // Accept an invite
    @PostMapping("/accept-invite")
    public ResponseEntity<?> acceptInvite(@RequestParam String token, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        User invitee = userRepository.findById(userId).orElseThrow();
        
        OrganizationMembership membership = inviteService.acceptInvite(token, invitee);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Invite accepted successfully");
        response.put("organizationId", membership.getOrganization().getId());
        response.put("organizationName", membership.getOrganization().getName());
        response.put("orgRole", membership.getRole().name());
        
        return ResponseEntity.ok(response);
    }
}
