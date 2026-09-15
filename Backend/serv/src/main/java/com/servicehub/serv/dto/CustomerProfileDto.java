package com.servicehub.serv.dto;

public class CustomerProfileDto {

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

    public CustomerProfileDto(
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
            String pinCode) {

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
}