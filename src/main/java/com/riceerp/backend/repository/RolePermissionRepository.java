package com.riceerp.backend.repository;

import com.riceerp.backend.entity.RolePermission;
import com.riceerp.backend.enums.OrgRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Organization o where o.id = :id")
    Optional<com.riceerp.backend.entity.Organization> lockOrganization(@org.springframework.data.repository.query.Param("id") Long id);

    List<RolePermission> findByOrganizationId(Long organizationId);

    List<RolePermission> findByOrganizationIdAndOrgRole(Long organizationId, OrgRole orgRole);

    Optional<RolePermission> findByOrganizationIdAndOrgRoleAndPermission(Long organizationId, OrgRole orgRole, String permission);

    boolean existsByOrganizationId(Long organizationId);

    void deleteByOrganizationId(Long organizationId);
}
