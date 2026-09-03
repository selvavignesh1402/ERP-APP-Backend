package com.riceerp.backend.dto;

import com.riceerp.backend.enums.PlatformRole;

public class UserSummary {

    private Long id;
    private String name;
    private String phoneNumber;
    private PlatformRole role;
    private boolean active;
    private boolean profileCompleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public PlatformRole getRole() {
        return role;
    }

    public void setRole(PlatformRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }
}
