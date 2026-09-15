package com.servicehub.serv.dto;

import java.util.UUID;

public class CustomerCategoryDto {

    private UUID categoryId;
    private String categoryName;

    public CustomerCategoryDto(UUID categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
}