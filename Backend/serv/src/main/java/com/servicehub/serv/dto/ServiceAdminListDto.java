package com.servicehub.serv.dto;

import java.util.UUID;

public class ServiceAdminListDto {

    private UUID serviceId;
    private String serviceName;
    private UUID categoryId;
    private String categoryName;
    private boolean active;

    public ServiceAdminListDto(
            UUID serviceId,
            String serviceName,
            UUID categoryId,
            String categoryName,
            boolean active
    ) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.active = active;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public boolean isActive() {
        return active;
    }
}