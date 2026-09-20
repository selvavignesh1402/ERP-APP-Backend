package com.riceerp.backend.entity;

import com.riceerp.backend.enums.OrgRole;
import jakarta.persistence.*;

@Entity
@Table(
    name = "role_permissions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_perm_org_role_perm", columnNames = {"organization_id", "org_role", "permission"})
    }
)
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId; // Nullable (NULL = global default template)

    @Enumerated(EnumType.STRING)
    @Column(name = "org_role", length = 32, nullable = false)
    private OrgRole orgRole;

    @Column(name = "permission", length = 64, nullable = false)
    private String permission;

    @Column(name = "allowed", nullable = false)
    private boolean allowed = true;

    public RolePermission() {
    }

    public RolePermission(Long organizationId, OrgRole orgRole, String permission, boolean allowed) {
        this.organizationId = organizationId;
        this.orgRole = orgRole;
        this.permission = permission;
        this.allowed = allowed;
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public OrgRole getOrgRole() {
        return orgRole;
    }

    public void setOrgRole(OrgRole orgRole) {
        this.orgRole = orgRole;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }
}
