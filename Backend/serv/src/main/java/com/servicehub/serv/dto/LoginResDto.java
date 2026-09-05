package com.servicehub.serv.dto;

import com.servicehub.serv.enums.UserRole;
import java.util.UUID;

public class LoginResDto {

    private UUID userId;
    private String email;
    private UserRole role;

    public LoginResDto(UUID userId, String email, UserRole role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    //getters

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }
}
