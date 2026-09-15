package com.servicehub.serv.dto;

import java.util.UUID;

public class CustomerServiceDto {

    private UUID serviceId;
    private String serviceName;
    private UUID categoryId;
    private String categoryName;

    public CustomerServiceDto(
            UUID serviceId,
            String serviceName,
            UUID categoryId,
            String categoryName
    ) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
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
}