package com.riceerp.backend.service;

import com.riceerp.backend.entity.Organization;
import com.riceerp.backend.entity.OrganizationInvite;
import com.riceerp.backend.entity.OrganizationMembership;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.OrgRole;
import com.riceerp.backend.repository.OrganizationInviteRepository;
import com.riceerp.backend.repository.OrganizationMembershipRepository;
import com.riceerp.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import com.riceerp.backend.exception.BusinessRuleException;

import java.util.Optional;
import java.util.UUID;

@Service
public class InviteService {

    private final OrganizationInviteRepository inviteRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    
    public InviteService(OrganizationInviteRepository inviteRepository,
                         OrganizationMembershipRepository membershipRepository,
                         UserRepository userRepository) {
        this.inviteRepository = inviteRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrganizationInvite createInvite(Organization organization, User invitedBy, String inviteePhoneNumber, OrgRole role) {
        // Prevent duplicate invites or inviting someone already in the org
        Optional<User> existingUser = userRepository.findByPhoneNumber(inviteePhoneNumber);
        if (existingUser.isPresent()) {
            Optional<OrganizationMembership> existingMembership = membershipRepository.findByUserIdAndOrganizationId(existingUser.get().getId(), organization.getId());
            if (existingMembership.isPresent()) {
                throw new BusinessRuleException("User is already a member of this organization");
            }
        }

        OrganizationInvite invite = new OrganizationInvite();
        invite.setOrganization(organization);
        invite.setInvitedBy(invitedBy);
        invite.setInviteePhoneNumber(inviteePhoneNumber);
        invite.setRole(role);
        invite.setToken(UUID.randomUUID().toString());
        invite.setStatus("PENDING");

        return inviteRepository.save(invite);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrganizationMembership acceptInvite(String token, User invitee) {
        OrganizationInvite invite = inviteRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired invite token"));

        if (invite.getExpiresAt() != null && java.time.LocalDateTime.now().isAfter(invite.getExpiresAt())) {
            if ("PENDING".equalsIgnoreCase(invite.getStatus())) {
                invite.setStatus("EXPIRED");
                inviteRepository.save(invite);
            }
            throw new BusinessRuleException("This invitation link has expired");
        }

        if (!"PENDING".equalsIgnoreCase(invite.getStatus())) {
            throw new BusinessRuleException("Invite has already been accepted or cancelled");
        }

        // Validate invite matches logged-in user's phone number
        if (!invite.getInviteePhoneNumber().equals(invitee.getPhoneNumber())) {
            throw new BusinessRuleException("This invite belongs to a different phone number");
        }

        if (membershipRepository.findByUserIdAndOrganizationId(invitee.getId(), invite.getOrganization().getId()).isPresent())
            throw new BusinessRuleException("You are already a member of this organization");
        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(invite.getOrganization());
        membership.setUser(invitee);
        membership.setRole(invite.getRole());

        invite.setStatus("ACCEPTED");
        inviteRepository.save(invite);

        return membershipRepository.saveAndFlush(membership);
    }
    
    @Transactional(readOnly = true)
    public OrganizationInvite getInvite(String token) {
        return inviteRepository.findByToken(token)
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired invite token"));
    }

    public java.util.List<OrganizationInvite> getInvitesForOrg(Long organizationId) {
        return inviteRepository.findByOrganizationId(organizationId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelInvite(Long inviteId, Long organizationId) {
        OrganizationInvite invite = inviteRepository.findForUpdate(inviteId, organizationId)
                .orElseThrow(() -> new BusinessRuleException("Invite not found with id: " + inviteId));
        if (!invite.getOrganization().getId().equals(organizationId)) {
            throw new BusinessRuleException("Unauthorized cross-tenant invite deletion");
        }
        if (!"PENDING".equals(invite.getStatus())) throw new BusinessRuleException("Only pending invitations can be cancelled");
        invite.setStatus("CANCELLED");
        inviteRepository.save(invite);
    }
}
