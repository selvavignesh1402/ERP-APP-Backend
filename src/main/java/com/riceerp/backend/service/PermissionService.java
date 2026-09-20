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
    
    // In-memory cache: "orgId:role" -> Set<String> permissions
    private final Map<String, CachedPermissions> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60_000; // 60s TTL

    private static class CachedPermissions {
        final Set<String> permissions;
        final long timestamp;

        CachedPermissions(Set<String> permissions) {
            this.permissions = permissions;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

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

        String cacheKey = orgId + ":" + orgRole.name();
        CachedPermissions cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.permissions;
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

        cache.put(cacheKey, new CachedPermissions(Collections.unmodifiableSet(effective)));
        return effective;
    }

    @Transactional
    public void seedPermissionsForOrg(Long orgId) {
        if (orgId == null) return;

        for (OrgRole role : OrgRole.values()) {
            Set<String> defaults = DefaultPermissionMatrix.getDefaults(role);
            for (String perm : DefaultPermissionMatrix.ALL_PERMISSIONS) {
                boolean allowed = defaults.contains(perm);
                Optional<RolePermission> existing = rolePermissionRepository
                        .findByOrganizationIdAndOrgRoleAndPermission(orgId, role, perm);
                
                if (existing.isEmpty()) {
                    rolePermissionRepository.save(new RolePermission(orgId, role, perm, allowed));
                }
            }
        }
        evictCacheForOrg(orgId);
    }

    @Transactional
    public void updatePermission(Long orgId, OrgRole role, String permission, boolean allowed) {
        if (orgId == null || role == null || permission == null) return;

        Optional<RolePermission> opt = rolePermissionRepository
                .findByOrganizationIdAndOrgRoleAndPermission(orgId, role, permission);

        if (opt.isPresent()) {
            RolePermission rp = opt.get();
            rp.setAllowed(allowed);
            rolePermissionRepository.save(rp);
        } else {
            rolePermissionRepository.save(new RolePermission(orgId, role, permission, allowed));
        }

        evictCacheForOrg(orgId);
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
                    roleMap.put(perm, fromDb.getOrDefault(perm, DefaultPermissionMatrix.isDefaultAllowed(role, perm)));
                }
            }
            matrix.put(role.name(), roleMap);
        }

        return matrix;
    }

    public void evictCacheForOrg(Long orgId) {
        if (orgId == null) {
            cache.clear();
            return;
        }
        for (OrgRole role : OrgRole.values()) {
            cache.remove(orgId + ":" + role.name());
        }
    }
}
