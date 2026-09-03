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

    public OrganizationInvite createInvite(Organization organization, User invitedBy, String inviteePhoneNumber, OrgRole role) {
        // Prevent duplicate invites or inviting someone already in the org
        Optional<User> existingUser = userRepository.findByPhoneNumber(inviteePhoneNumber);
        if (existingUser.isPresent()) {
            Optional<OrganizationMembership> existingMembership = membershipRepository.findByUserIdAndOrganizationId(existingUser.get().getId(), organization.getId());
            if (existingMembership.isPresent()) {
                throw new RuntimeException("User is already a member of this organization");
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

    public OrganizationMembership acceptInvite(String token, User invitee) {
        OrganizationInvite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired invite token"));

        if (!invite.getStatus().equals("PENDING")) {
            throw new RuntimeException("Invite has already been accepted or cancelled");
        }

        // Validate invite matches logged-in user's phone number
        if (!invite.getInviteePhoneNumber().equals(invitee.getPhoneNumber())) {
            throw new RuntimeException("This invite belongs to a different phone number");
        }

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(invite.getOrganization());
        membership.setUser(invitee);
        membership.setRole(invite.getRole());

        invite.setStatus("ACCEPTED");
        inviteRepository.save(invite);

        return membershipRepository.save(membership);
    }
    
    public OrganizationInvite getInvite(String token) {
        return inviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired invite token"));
    }

    public java.util.List<OrganizationInvite> getInvitesForOrg(Long organizationId) {
        return inviteRepository.findByOrganizationId(organizationId);
    }

    public void cancelInvite(Long inviteId, Long organizationId) {
        OrganizationInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new RuntimeException("Invite not found with id: " + inviteId));
        if (!invite.getOrganization().getId().equals(organizationId)) {
            throw new RuntimeException("Unauthorized cross-tenant invite deletion");
        }
        invite.setStatus("CANCELLED");
        inviteRepository.save(invite);
    }
}
