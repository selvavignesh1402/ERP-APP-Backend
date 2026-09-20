package com.riceerp.backend.security;

import java.nio.charset.StandardCharsets;

/** Password requirements for newly provisioned accounts; existing credentials are never reset. */
public final class ProvisioningPasswordPolicy {
    private ProvisioningPasswordPolicy() {}

    public static void validate(String password) {
        if (password == null || password.trim().length() < 12) {
            throw new IllegalArgumentException("A password of at least 12 characters is required for a new account.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
        }
    }
}
