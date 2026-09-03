package com.riceerp.backend.config;

import com.riceerp.backend.enums.PlatformRole;
import com.riceerp.backend.entity.User;
import com.riceerp.backend.entity.Organization;
import com.riceerp.backend.entity.OrganizationMembership;
import com.riceerp.backend.enums.OrgRole;
import com.riceerp.backend.repository.OrganizationRepository;
import com.riceerp.backend.repository.OrganizationMembershipRepository;
import com.riceerp.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, 
                           OrganizationRepository organizationRepository,
                           OrganizationMembershipRepository membershipRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Init Default Organization
        Organization defaultOrg;
        if (organizationRepository.findById(1L).isEmpty()) {
            defaultOrg = new Organization();
            defaultOrg.setName("Default Retailer");
            defaultOrg = organizationRepository.save(defaultOrg);
            System.out.println("Default Organization created");
        } else {
            defaultOrg = organizationRepository.findById(1L).get();
        }

        // Init Default Admin User
        String adminPhone = "9999999999";
        if (!userRepository.existsByPhoneNumber(adminPhone)) {
            User admin = new User();
            admin.setName("Default Admin");
            admin.setPhoneNumber(adminPhone);
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setProfileCompleted(true);
            admin.setActive(true);
            admin.setPlatformRole(PlatformRole.MASTER_ADMIN);

            admin = userRepository.save(admin);
            
            OrganizationMembership membership = new OrganizationMembership();
            membership.setOrganization(defaultOrg);
            membership.setUser(admin);
            membership.setRole(OrgRole.ADMIN);
            membershipRepository.save(membership);
            
            System.out.println("Default Admin User created with Phone: " + adminPhone + " and Password: admin123");
        }
    }
}
