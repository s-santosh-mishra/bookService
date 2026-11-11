package com.servicehub.serv.dto;

import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.VerificationStatus;

import java.util.List;

public class WorkerProfileDto {

    private String fullName;
    private String email;
    private Integer age;
    private String gender;
    private String phone;

    private String addressLine1;
    private String addressLine2;
    private String landmark;
    private String city;
    private String state;
    private String pinCode;

    private Integer experienceYears;
    private String qualification;
    private String bio;

    private VerificationStatus verificationStatus;
    private AvailabilityStatus availabilityStatus;
    private List<String> services;

    private String bankAccountHolderName;
    private String maskedBankAccountNumber;
    private String bankIfscCode;
    private String bankName;

    public WorkerProfileDto(
            String fullName,
            String email,
            Integer age,
            String gender,
            String phone,
            String addressLine1,
            String addressLine2,
            String landmark,
            String city,
            String state,
            String pinCode,
            Integer experienceYears,
            String qualification,
            String bio,
            VerificationStatus verificationStatus,
            AvailabilityStatus availabilityStatus,
            List<String> services,
            String bankAccountHolderName,
            String bankAccountNumber,
            String bankIfscCode,
            String bankName) {

        this.fullName = fullName;
        this.email = email;
        this.age = age;
        this.gender = gender;
        this.phone = phone;

        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.landmark = landmark;
        this.city = city;
        this.state = state;
        this.pinCode = pinCode;

        this.experienceYears = experienceYears;
        this.qualification = qualification;
        this.bio = bio;

        this.verificationStatus = verificationStatus;
        this.availabilityStatus = availabilityStatus;
        this.services = services;
        this.bankAccountHolderName = bankAccountHolderName;
        this.maskedBankAccountNumber = BankAccountMasker.mask(bankAccountNumber);
        this.bankIfscCode = bankIfscCode;
        this.bankName = bankName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Integer getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
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

    public List<String> getServices() {
        return services;
    }

    public String getBankAccountHolderName() {
        return bankAccountHolderName;
    }

    public void setBankAccountHolderName(String bankAccountHolderName) {
        this.bankAccountHolderName = bankAccountHolderName;
    }

    public String getMaskedBankAccountNumber() {
        return maskedBankAccountNumber;
    }

    public void setMaskedBankAccountNumber(String maskedBankAccountNumber) {
        this.maskedBankAccountNumber = maskedBankAccountNumber;
    }

    public String getBankIfscCode() {
        return bankIfscCode;
    }

    public void setBankIfscCode(String bankIfscCode) {
        this.bankIfscCode = bankIfscCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
}