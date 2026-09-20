package com.riceerp.backend.config;

import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.PlatformRole;
import com.riceerp.backend.repository.UserRepository;
import com.riceerp.backend.security.ProvisioningPasswordPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit first-install bootstrap. Disabled unless deliberately enabled by deployment configuration. */
@Component
@ConditionalOnProperty(prefix = "app.bootstrap.master-admin", name = "enabled", havingValue = "true")
public class MasterAdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String phone;
    private final String name;
    private final String password;

    public MasterAdminBootstrap(UserRepository users, PasswordEncoder encoder,
            @Value("${app.bootstrap.master-admin.phone:}") String phone,
            @Value("${app.bootstrap.master-admin.name:}") String name,
            @Value("${app.bootstrap.master-admin.password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.phone = phone;
        this.name = name;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Never recreate, reactivate, reset, or add an administrator on subsequent starts.
        if (users.countByPlatformRole(PlatformRole.MASTER_ADMIN) > 0) return;
        if (phone.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("Master admin bootstrap requires an explicit phone number and name.");
        }
        ProvisioningPasswordPolicy.validate(password);
        if (users.existsByPhoneNumber(phone.trim())) {
            throw new IllegalArgumentException("Bootstrap phone already belongs to an account; automatic promotion is not allowed.");
        }
        User admin = new User();
        admin.setName(name.trim());
        admin.setPhoneNumber(phone.trim());
        admin.setPasswordHash(encoder.encode(password));
        admin.setPlatformRole(PlatformRole.MASTER_ADMIN);
        admin.setActive(true);
        admin.setProfileCompleted(true);
        users.save(admin);
    }
}
