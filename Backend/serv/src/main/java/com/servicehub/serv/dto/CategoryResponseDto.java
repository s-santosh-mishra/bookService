package com.servicehub.serv.dto;

import java.util.List;
import java.util.UUID;

public class CategoryResponseDto {

    private UUID categoryId;
    private String categoryName;
    private List<ServiceResponseDto> services;

    public CategoryResponseDto() {
    }

    public CategoryResponseDto(
            UUID categoryId,
            String categoryName,
            List<ServiceResponseDto> services
    ) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.services = services;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<ServiceResponseDto> getServices() {
        return services;
    }

    public void setServices(List<ServiceResponseDto> services) {
        this.services = services;
    }
}