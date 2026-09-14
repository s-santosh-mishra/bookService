package com.servicehub.serv.dto;

public class AdminActionResponseDto {

    private String message;

    public AdminActionResponseDto(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}