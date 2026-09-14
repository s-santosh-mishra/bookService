package com.servicehub.serv.dto;

import java.util.UUID;

public class ServiceWorkersAdminDto {

    private UUID workerId;
    private String fullName;
    private String email;
    private String phone;
    private int experienceYears;
    private String verificationStatus;
    private String availabilityStatus;

    public ServiceWorkersAdminDto(
            UUID workerId,
            String fullName,
            String email,
            String phone,
            int experienceYears,
            String verificationStatus,
            String availabilityStatus
    ) {
        this.workerId = workerId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.experienceYears = experienceYears;
        this.verificationStatus = verificationStatus;
        this.availabilityStatus = availabilityStatus;
    }

    public UUID getWorkerId() {
        return workerId;
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

    public int getExperienceYears() {
        return experienceYears;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }
}