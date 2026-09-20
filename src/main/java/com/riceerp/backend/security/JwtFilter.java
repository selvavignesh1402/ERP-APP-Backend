package com.riceerp.backend.security;

import com.riceerp.backend.entity.OrganizationMembership;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.OrgRole;
import com.riceerp.backend.enums.PlatformRole;
import com.riceerp.backend.repository.OrganizationMembershipRepository;
import com.riceerp.backend.repository.UserRepository;
import com.riceerp.backend.service.PermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JwtFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final PermissionService permissionService;

    public JwtFilter(UserRepository userRepository,
                     OrganizationMembershipRepository membershipRepository,
                     PermissionService permissionService) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.permissionService = permissionService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    Long userId = JwtUtil.extractUserId(token);

                    User user = userRepository.findById(userId).orElse(null);
                    if (user == null || !user.isActive()) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }

                    String pRoleStr = JwtUtil.extractPlatformRole(token);
                    PlatformRole platformRole = "MASTER_ADMIN".equals(pRoleStr) || user.getPlatformRole() == PlatformRole.MASTER_ADMIN
                            ? PlatformRole.MASTER_ADMIN
                            : PlatformRole.USER;

                    Long orgId = JwtUtil.extractOrganizationId(token);
                    OrgRole activeOrgRole = null;

                    // 1. If organizationId is present, verify active membership
                    if (orgId != null) {
                        Optional<OrganizationMembership> membershipOpt =
                                membershipRepository.findByUserIdAndOrganizationIdAndIsActiveTrue(userId, orgId);

                        if (membershipOpt.isEmpty() && platformRole != PlatformRole.MASTER_ADMIN) {
                            // Membership was deleted or deactivated -> kill stale token
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            return;
                        }

                        if (membershipOpt.isPresent()) {
                            activeOrgRole = membershipOpt.get().getRole();
                            TenantContext.setCurrentTenant(orgId);
                        } else if (platformRole == PlatformRole.MASTER_ADMIN) {
                            TenantContext.setCurrentTenant(orgId);
                        }
                    } else {
                        // Fail-closed tenant check: memberless token cannot access tenant data
                        String path = request.getRequestURI();
                        boolean isPublicOrMemberlessPath =
                                path.startsWith("/auth/") ||
                                path.startsWith("/api/auth/") ||
                                path.startsWith("/profile") ||
                                path.startsWith("/api/master-admin") ||
                                path.equals("/api/organizations/my") ||
                                path.equals("/api/organizations/select") ||
                                path.startsWith("/api/organizations/invite-details") ||
                                path.startsWith("/api/organizations/accept-invite") ||
                                path.startsWith("/api/organizations/my-invites") ||
                                (path.equals("/api/organizations") && "POST".equalsIgnoreCase(request.getMethod())) ||
                                path.equals("/error");

                        if (!isPublicOrMemberlessPath && platformRole != PlatformRole.MASTER_ADMIN) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            return;
                        }
                    }

                    // 2. Build Granted Authorities
                    List<GrantedAuthority> authorities = new ArrayList<>();

                    // Platform authorities
                    authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_" + platformRole.name()));
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + platformRole.name()));

                    if (platformRole == PlatformRole.MASTER_ADMIN) {
                        // Master Admin gets all canonical permissions
                        for (String perm : DefaultPermissionMatrix.ALL_PERMISSIONS) {
                            authorities.add(new SimpleGrantedAuthority(perm));
                        }
                        authorities.add(new SimpleGrantedAuthority("ROLE_ORG_ADMIN"));
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }

                    // Organizational authorities & granular permissions
                    if (activeOrgRole != null && orgId != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ORG_" + activeOrgRole.name()));
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + activeOrgRole.name()));

                        Set<String> perms = permissionService.getEffectivePermissions(orgId, activeOrgRole);
                        for (String perm : perms) {
                            authorities.add(new SimpleGrantedAuthority(perm));
                        }
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
