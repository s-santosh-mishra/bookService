package com.servicehub.serv.dto;

import java.util.UUID;

public class CategoryAdminListDto {

    private UUID categoryId;
    private String categoryName;
    private boolean active;
    private long serviceCount;

    public CategoryAdminListDto(
            UUID categoryId,
            String categoryName,
            boolean active,
            long serviceCount
    ) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.active = active;
        this.serviceCount = serviceCount;
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

    public long getServiceCount() {
        return serviceCount;
    }
}