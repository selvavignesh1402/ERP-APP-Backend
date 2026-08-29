package com.riceerp.backend.dto;

import com.riceerp.backend.enums.Role;

public class UpdateUserRequest {

    private Role role;
    private Boolean active;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
