package com.servicehub.serv.dto;

import com.servicehub.serv.enums.UserRole;

import java.util.UUID;

public class LoginResDto {

    private UUID userId;
    private String email;
    private UserRole role;
    private String token;

    public LoginResDto(
            UUID userId,
            String email,
            UserRole role,
            String token
    ) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.token = token;
    }

    // Getters

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }
}