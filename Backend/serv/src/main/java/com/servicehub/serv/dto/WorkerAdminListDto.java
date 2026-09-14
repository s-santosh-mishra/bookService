package com.servicehub.serv.dto;

import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.VerificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class WorkerAdminListDto {

    private UUID workerId;
    private String fullName;
    private String email;
    private String phone;
    private Integer experienceYears;
    private String qualification;
    private String bio;
    private VerificationStatus verificationStatus;
    private AvailabilityStatus availabilityStatus;
    private LocalDateTime createdAt;


    public WorkerAdminListDto(
            UUID workerId,
            String fullName,
            String email,
            String phone,
            Integer experienceYears,
            String qualification,
            String bio,
            VerificationStatus verificationStatus,
            AvailabilityStatus availabilityStatus,
            LocalDateTime createdAt
    ) {
        this.workerId = workerId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.experienceYears = experienceYears;
        this.qualification = qualification;
        this.bio = bio;
        this.verificationStatus = verificationStatus;
        this.availabilityStatus = availabilityStatus;
        this.createdAt = createdAt;
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

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public String getQualification() {
        return qualification;
    }

    public String getBio() {
        return bio;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}