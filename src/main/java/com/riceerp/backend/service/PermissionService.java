package com.riceerp.backend.service;

import com.riceerp.backend.entity.RolePermission;
import com.riceerp.backend.enums.OrgRole;
import com.riceerp.backend.repository.RolePermissionRepository;
import com.riceerp.backend.security.DefaultPermissionMatrix;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    
    public PermissionService(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public Set<String> getEffectivePermissions(Long orgId, OrgRole orgRole) {
        if (orgRole == null) {
            return Collections.emptySet();
        }

        if (orgId == null) {
            // Global/Platform token context with no organization selected
            return Collections.emptySet();
        }

        List<RolePermission> dbRows = rolePermissionRepository.findByOrganizationIdAndOrgRole(orgId, orgRole);

        Set<String> effective;
        if (dbRows.isEmpty()) {
            // Fallback to canonical default matrix if no custom rows seeded yet
            effective = new HashSet<>(DefaultPermissionMatrix.getDefaults(orgRole));
        } else {
            effective = new HashSet<>();
            for (RolePermission rp : dbRows) {
                if (rp.isAllowed()) {
                    effective.add(rp.getPermission());
                }
            }
        }

        return Collections.unmodifiableSet(effective);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void seedPermissionsForOrg(Long orgId) {
        if (orgId == null) return;
        rolePermissionRepository.lockOrganization(orgId).orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        for (OrgRole role : OrgRole.values()) {
            Set<String> defaults = DefaultPermissionMatrix.getDefaults(role);
            for (String perm : DefaultPermissionMatrix.ALL_PERMISSIONS) {
                boolean allowed = defaults.contains(perm);
                Optional<RolePermission> existing = rolePermissionRepository
                        .findByOrganizationIdAndOrgRoleAndPermission(orgId, role, perm);
                
                if (existing.isEmpty()) {
                    // Introducing visit mutation permission must not undo an existing
                    // restriction on a role's access to field visits.
                    if ("visit:execute".equals(perm)) {
                        allowed = allowed && rolePermissionRepository
                                .findByOrganizationIdAndOrgRoleAndPermission(orgId, role, "beat-plan:view")
                                .map(RolePermission::isAllowed).orElse(true);
                    }
                    rolePermissionRepository.save(new RolePermission(orgId, role, perm, allowed));
                }
            }
        }
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void updatePermission(Long orgId, OrgRole role, String permission, boolean allowed) {
        if (orgId == null || role == null || !DefaultPermissionMatrix.ALL_PERMISSIONS.contains(permission))
            throw new IllegalArgumentException("A known role and permission are required");
        if (role == OrgRole.ADMIN && !allowed && "member:manage".equals(permission))
            throw new IllegalArgumentException("Administrators must retain permission management");
        rolePermissionRepository.lockOrganization(orgId).orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        Optional<RolePermission> opt = rolePermissionRepository
                .findByOrganizationIdAndOrgRoleAndPermission(orgId, role, permission);

        if (opt.isPresent()) {
            RolePermission rp = opt.get();
            rp.setAllowed(allowed);
            rolePermissionRepository.save(rp);
        } else {
            rolePermissionRepository.save(new RolePermission(orgId, role, permission, allowed));
        }

    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void updateMatrix(Long orgId, List<Map<String, Object>> updates) {
        if (updates == null || updates.isEmpty()) throw new IllegalArgumentException("Permission updates are required");
        // Validate the whole batch before mutating rows.
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> update : updates) {
            if (update == null || !(update.get("role") instanceof String role) ||
                    !(update.get("permission") instanceof String permission) || !(update.get("allowed") instanceof Boolean))
                throw new IllegalArgumentException("Each update requires role, permission and allowed");
            OrgRole.valueOf(role.toUpperCase(Locale.ROOT));
            if (!DefaultPermissionMatrix.ALL_PERMISSIONS.contains(permission) || !seen.add(role.toUpperCase(Locale.ROOT) + ":" + permission))
                throw new IllegalArgumentException("Unknown or duplicate permission update");
        }
        rolePermissionRepository.lockOrganization(orgId).orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        for (Map<String, Object> update : updates)
            updatePermission(orgId, OrgRole.valueOf(((String) update.get("role")).toUpperCase(Locale.ROOT)),
                    (String) update.get("permission"), (Boolean) update.get("allowed"));
    }

    public Map<String, Map<String, Boolean>> getMatrixForOrg(Long orgId) {
        Map<String, Map<String, Boolean>> matrix = new LinkedHashMap<>();

        for (OrgRole role : OrgRole.values()) {
            Map<String, Boolean> roleMap = new LinkedHashMap<>();
            List<RolePermission> dbRows = rolePermissionRepository.findByOrganizationIdAndOrgRole(orgId, role);

            if (dbRows.isEmpty()) {
                Set<String> defaults = DefaultPermissionMatrix.getDefaults(role);
                for (String perm : DefaultPermissionMatrix.ALL_PERMISSIONS) {
                    roleMap.put(perm, defaults.contains(perm));
                }
            } else {
                Map<String, Boolean> fromDb = new HashMap<>();
                for (RolePermission rp : dbRows) {
                    fromDb.put(rp.getPermission(), rp.isAllowed());
                }
                for (String perm : DefaultPermissionMatrix.ALL_PERMISSIONS) {
                    roleMap.put(perm, fromDb.getOrDefault(perm, false));
                }
            }
            matrix.put(role.name(), roleMap);
        }

        return matrix;
    }

}

