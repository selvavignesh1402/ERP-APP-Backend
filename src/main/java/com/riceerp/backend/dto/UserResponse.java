package com.riceerp.backend.dto;

import com.riceerp.backend.entity.User;
import com.riceerp.backend.enums.*;
import java.time.*;
import java.util.List;

/** Explicit public contract. Persistence metadata and back references are not exposed. */
public record UserResponse(
        Long id,
        String phoneNumber,
        String name,
        boolean profileCompleted,
        boolean active,
        LocalDateTime createdAt,
        PlatformRole platformRole) {
    public static UserResponse from(User e) {
        if (e == null) return null;
        return new UserResponse(
                e.getId(),
                e.getPhoneNumber(),
                e.getName(),
                e.isProfileCompleted(),
                e.isActive(),
                e.getCreatedAt(),
                e.getPlatformRole());
    }
}

