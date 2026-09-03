package com.riceerp.backend.dto;

import com.riceerp.backend.enums.PlatformRole;

public class UpdateUserRequest {

    private PlatformRole role;
    private Boolean active;

    public PlatformRole getRole() {
        return role;
    }

    public void setRole(PlatformRole role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
