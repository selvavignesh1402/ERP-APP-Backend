package com.riceerp.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class FirebaseLoginRequest {

    @NotBlank(message = "Firebase ID token is required")
    private String token;

    private String name;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
