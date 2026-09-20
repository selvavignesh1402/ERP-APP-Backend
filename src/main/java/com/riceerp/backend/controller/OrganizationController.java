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
import com.riceerp.backend.security.TenantContext;
import com.riceerp.backend.security.ProvisioningPasswordPolicy;
import com.riceerp.backend.service.InviteService;
import com.riceerp.backend.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final PermissionService permissionService;

    public OrganizationController(OrganizationMembershipRepository membershipRepository,
                                  OrganizationRepository organizationRepository,
                                  InviteService inviteService,
                                  UserRepository userRepository,
                                  PermissionService permissionService) {
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.inviteService = inviteService;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
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
            map.put("isActive", m.isActive());
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
        org = organizationRepository.save(org);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(OrgRole.ADMIN); // The creator is the ADMIN
        membership.setActive(true);
        membershipRepository.save(membership);

        // Seed default permission matrix for the new organization
        permissionService.seedPermissionsForOrg(org.getId());
        
        String token = JwtUtil.generateToken(userId, user.getPhoneNumber(), user.getPlatformRole(), org.getId(), OrgRole.ADMIN);
        
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
        
        // Verify user has an active membership
        OrganizationMembership membership = membershipRepository.findByUserIdAndOrganizationIdAndIsActiveTrue(userId, organizationId)
                .orElseThrow(() -> new RuntimeException("User does not have an active membership in this organization"));

        User user = userRepository.findById(userId).orElseThrow();
        String token = JwtUtil.generateToken(userId, user.getPhoneNumber(), user.getPlatformRole(), organizationId, membership.getRole());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("organizationId", organizationId);
        response.put("orgRole", membership.getRole().name());
        
        return ResponseEntity.ok(response);
    }

    // ─── TEAM & STAFF MANAGEMENT (Strict Multi-Tenancy Scoped) ───

    // List all team members in current organization
    @GetMapping("/members")
    @PreAuthorize("hasAuthority('member:view')")
    public List<Map<String, Object>> getOrganizationMembers() {
        Long orgId = TenantContext.getCurrentTenant();
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
    @PreAuthorize("hasAuthority('member:manage')")
    public ResponseEntity<?> createStaffDirectly(@RequestBody Map<String, String> request) {
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No active organization context found");
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
            ProvisioningPasswordPolicy.validate(password);
            user = new User();
            user.setName(name.trim());
            user.setPhoneNumber(phone.trim());
            user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password));
            user.setPlatformRole(com.riceerp.backend.enums.PlatformRole.USER);
            user = userRepository.save(user);
        } else {
            // Check if already in this org
            Optional<OrganizationMembership> existing = membershipRepository.findByUserIdAndOrganizationId(user.getId(), orgId);
            if (existing.isPresent()) {
                throw new RuntimeException("This staff member is already part of the shop!");
            }
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

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
    @PreAuthorize("hasAuthority('member:manage')")
    public ResponseEntity<?> updateMemberRole(@PathVariable Long membershipId, @RequestBody Map<String, String> request) {
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No active organization context found");
        }

        OrganizationMembership target = membershipRepository.findByIdAndOrganizationId(membershipId, orgId)
                .orElseThrow(() -> new RuntimeException("Member not found in your organization"));

        String newRoleStr = request.get("role");
        OrgRole newRole = OrgRole.valueOf(newRoleStr.toUpperCase());

        // Last Admin Guard: prevent demoting the last active ADMIN
        if (target.getRole() == OrgRole.ADMIN && newRole != OrgRole.ADMIN) {
            assertNotLastAdmin(orgId, target.getId());
        }

        target.setRole(newRole);
        membershipRepository.save(target);

        return ResponseEntity.ok(Map.of("message", "Staff role updated to " + target.getRole().name()));
    }

    // Toggle Staff Active / Deactivated Status
    @PutMapping("/members/{membershipId}/status")
    @PreAuthorize("hasAuthority('member:manage')")
    public ResponseEntity<?> toggleMemberStatus(@PathVariable Long membershipId, @RequestBody Map<String, Boolean> request) {
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No active organization context found");
        }

        OrganizationMembership target = membershipRepository.findByIdAndOrganizationId(membershipId, orgId)
                .orElseThrow(() -> new RuntimeException("Member not found in your organization"));

        Boolean targetActive = request.get("isActive") != null ? request.get("isActive") : !target.isActive();

        // Last Admin Guard: prevent deactivating the last active ADMIN
        if (target.getRole() == OrgRole.ADMIN && !targetActive) {
            assertNotLastAdmin(orgId, target.getId());
        }

        target.setActive(targetActive);
        membershipRepository.save(target);

        return ResponseEntity.ok(Map.of("message", "Staff account status updated", "isActive", target.isActive()));
    }

    // Remove Staff Member from Organization
    @DeleteMapping("/members/{membershipId}")
    @PreAuthorize("hasAuthority('member:manage')")
    public ResponseEntity<?> removeMember(@PathVariable Long membershipId, Authentication authentication) {
        Long currentUserId = Long.parseLong(authentication.getPrincipal().toString());
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No active organization context found");
        }

        OrganizationMembership target = membershipRepository.findByIdAndOrganizationId(membershipId, orgId)
                .orElseThrow(() -> new RuntimeException("Member not found in your organization"));

        if (target.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("You cannot remove your own account from the organization");
        }

        // Last Admin Guard: prevent deleting the last active ADMIN
        if (target.getRole() == OrgRole.ADMIN) {
            assertNotLastAdmin(orgId, target.getId());
        }

        membershipRepository.delete(target);
        return ResponseEntity.ok(Map.of("message", "Staff member removed successfully"));
    }

    // Invite a new staff member
    @PostMapping("/invite")
    @PreAuthorize("hasAuthority('member:manage')")
    public ResponseEntity<?> inviteStaff(@RequestBody Map<String, String> request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No organization context found");
        }

        String phone = request.get("phoneNumber");
        OrgRole role = OrgRole.valueOf(request.get("role").toUpperCase());
        User inviter = userRepository.findById(userId).orElseThrow();
        Organization org = organizationRepository.findById(orgId).orElseThrow();
        
        OrganizationInvite invite = inviteService.createInvite(org, inviter, phone, role);
        
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
    @PreAuthorize("hasAuthority('member:view')")
    public List<Map<String, Object>> getPendingInvites() {
        Long orgId = TenantContext.getCurrentTenant();
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
    @PreAuthorize("hasAuthority('member:manage')")
    public ResponseEntity<?> cancelInvite(@PathVariable Long inviteId) {
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No organization context found");
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

    // ─── ROLE PERMISSION MATRIX MANAGEMENT (Strict Multi-Tenancy Scoped) ───

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('member:manage')")
    public ResponseEntity<?> getOrganizationPermissions() {
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No active organization context found");
        }

        return ResponseEntity.ok(permissionService.getMatrixForOrg(orgId));
    }

    @PutMapping("/permissions")
    @PreAuthorize("hasAuthority('member:manage')")
    public ResponseEntity<?> updateOrganizationPermissions(@RequestBody List<Map<String, Object>> updates) {
        Long orgId = TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new RuntimeException("No active organization context found");
        }

        for (Map<String, Object> u : updates) {
            String roleStr = (String) u.get("role");
            String permission = (String) u.get("permission");
            Boolean allowed = (Boolean) u.get("allowed");

            if (roleStr != null && permission != null && allowed != null) {
                OrgRole role = OrgRole.valueOf(roleStr.toUpperCase());
                permissionService.updatePermission(orgId, role, permission, allowed);
            }
        }

        return ResponseEntity.ok(Map.of("message", "Permissions updated successfully for your organization"));
    }

    private void assertNotLastAdmin(Long orgId, Long targetMembershipId) {
        long activeAdminCount = membershipRepository.findByOrganizationId(orgId).stream()
                .filter(m -> m.isActive() && m.getRole() == OrgRole.ADMIN && !m.getId().equals(targetMembershipId))
                .count();

        if (activeAdminCount == 0) {
            throw new RuntimeException("Cannot demote, deactivate, or delete the last active Admin of the organization.");
        }
    }
}
