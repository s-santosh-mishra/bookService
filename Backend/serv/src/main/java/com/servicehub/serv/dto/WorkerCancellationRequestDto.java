package com.servicehub.serv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WorkerCancellationRequestDto {

    @NotBlank
    @Size(max = 100)
    private String reason;

    @Size(max = 1000)
    private String message;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}