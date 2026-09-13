package com.servicehub.serv.dto;

import java.util.UUID;

public class ServiceResponseDto {

    private UUID serviceId;
    private String serviceName;

    public ServiceResponseDto() {
    }

    public ServiceResponseDto(UUID serviceId, String serviceName) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}