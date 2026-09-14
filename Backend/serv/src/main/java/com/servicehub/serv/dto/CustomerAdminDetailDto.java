package com.servicehub.serv.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class CustomerAdminDetailDto {

    private UUID customerId;

    private String fullName;
    private String email;
    private String phone;

    private Integer age;
    private String gender;

    private String addressLine1;
    private String addressLine2;
    private String landmark;
    private String city;
    private String state;
    private String pinCode;

    private boolean active;
    private boolean termsAccepted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public CustomerAdminDetailDto(
            UUID customerId,
            String fullName,
            String email,
            String phone,
            Integer age,
            String gender,
            String addressLine1,
            String addressLine2,
            String landmark,
            String city,
            String state,
            String pinCode,
            boolean active,
            boolean termsAccepted,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.age = age;
        this.gender = gender;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.landmark = landmark;
        this.city = city;
        this.state = state;
        this.pinCode = pinCode;
        this.active = active;
        this.termsAccepted = termsAccepted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Integer getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getLandmark() {
        return landmark;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPinCode() {
        return pinCode;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}