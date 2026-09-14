package com.servicehub.serv.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class CustomerAdminListDto {

    private UUID customerId;
    private String fullName;
    private String email;
    private String phone;
    private String city;
    private boolean active;
    private LocalDateTime createdAt;


    public CustomerAdminListDto(
            UUID customerId,
            String fullName,
            String email,
            String phone,
            String city,
            boolean active,
            LocalDateTime createdAt
    ) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.city = city;
        this.active = active;
        this.createdAt = createdAt;
    }


    public UUID getCustomerId() {
        return customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCity() {
        return city;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
