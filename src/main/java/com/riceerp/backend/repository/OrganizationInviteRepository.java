package com.riceerp.backend.repository;

import com.riceerp.backend.entity.OrganizationInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationInviteRepository extends JpaRepository<OrganizationInvite, Long> {
    
    Optional<OrganizationInvite> findByToken(String token);

    java.util.List<OrganizationInvite> findByOrganizationId(Long organizationId);
}
