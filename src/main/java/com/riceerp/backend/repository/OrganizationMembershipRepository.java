package com.riceerp.backend.repository;

import com.riceerp.backend.entity.OrganizationMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {
    
    List<OrganizationMembership> findByUserId(Long userId);
    
    List<OrganizationMembership> findByOrganizationId(Long organizationId);

    List<OrganizationMembership> findAllByOrganizationIdAndIsActiveTrue(Long organizationId);
    
    Optional<OrganizationMembership> findByUserIdAndOrganizationId(Long userId, Long organizationId);

    Optional<OrganizationMembership> findByUserIdAndOrganizationIdAndIsActiveTrue(Long userId, Long organizationId);

    Optional<OrganizationMembership> findByIdAndOrganizationId(Long id, Long organizationId);

    long countByOrganizationId(Long organizationId);
}
