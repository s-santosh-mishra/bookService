package com.servicehub.serv.dto;

import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.VerificationStatus;

public class WorkerStatusDto {

    private VerificationStatus verificationStatus;
    private AvailabilityStatus availabilityStatus;


    public WorkerStatusDto() {
    }


    public WorkerStatusDto(
            VerificationStatus verificationStatus,
            AvailabilityStatus availabilityStatus) {

        this.verificationStatus = verificationStatus;
        this.availabilityStatus = availabilityStatus;
    }


    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }


    public void setVerificationStatus(
            VerificationStatus verificationStatus) {

        this.verificationStatus = verificationStatus;
    }


    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }


    public void setAvailabilityStatus(
            AvailabilityStatus availabilityStatus) {

        this.availabilityStatus = availabilityStatus;
    }
}