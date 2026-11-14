package com.servicehub.serv.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class CustomerServiceDto {

    private UUID serviceId;
    private String serviceName;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal basePricePerHour;
    private BigDecimal minimumServiceFee;

    public CustomerServiceDto() {
    }

    public CustomerServiceDto(
            UUID serviceId,
            String serviceName,
            UUID categoryId,
            String categoryName
    ) {
        this(serviceId, serviceName, categoryId, categoryName, null, null);
    }

    public CustomerServiceDto(
            UUID serviceId,
            String serviceName,
            UUID categoryId,
            String categoryName,
            BigDecimal basePricePerHour,
            BigDecimal minimumServiceFee
    ) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.basePricePerHour = basePricePerHour;
        this.minimumServiceFee = minimumServiceFee;
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

    public BigDecimal getBasePricePerHour() {
        return basePricePerHour;
    }

    public void setBasePricePerHour(BigDecimal basePricePerHour) {
        this.basePricePerHour = basePricePerHour;
    }

    public BigDecimal getMinimumServiceFee() {
        return minimumServiceFee;
    }

    public void setMinimumServiceFee(BigDecimal minimumServiceFee) {
        this.minimumServiceFee = minimumServiceFee;
    }
}
