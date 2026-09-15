package com.servicehub.serv.dto;

public class WorkerServiceDto {

    private String serviceName;

    public WorkerServiceDto(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}