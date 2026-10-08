package com.riceerp.backend.repository;

import com.riceerp.backend.entity.OrganizationInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationInviteRepository extends JpaRepository<OrganizationInvite, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from OrganizationInvite i where i.token = :token")
    Optional<OrganizationInvite> findByTokenForUpdate(@org.springframework.data.repository.query.Param("token") String token);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from OrganizationInvite i where i.id = :id and i.organization.id = :orgId")
    Optional<OrganizationInvite> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id,
                                           @org.springframework.data.repository.query.Param("orgId") Long orgId);
    
    Optional<OrganizationInvite> findByToken(String token);

    java.util.List<OrganizationInvite> findByOrganizationId(Long organizationId);
}
