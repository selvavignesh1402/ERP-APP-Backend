package com.riceerp.backend.repository;

import com.riceerp.backend.entity.RolePermission;
import com.riceerp.backend.enums.OrgRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByOrganizationId(Long organizationId);

    List<RolePermission> findByOrganizationIdAndOrgRole(Long organizationId, OrgRole orgRole);

    Optional<RolePermission> findByOrganizationIdAndOrgRoleAndPermission(Long organizationId, OrgRole orgRole, String permission);

    boolean existsByOrganizationId(Long organizationId);

    void deleteByOrganizationId(Long organizationId);
}
