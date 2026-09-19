package com.servicehub.serv.entity;

import jakarta.persistence.*;

import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "services")
public class Service {

    @Id
    @GeneratedValue
    @Column(name = "service_id")
    private UUID serviceId;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "base_price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePricePerHour;

    @Column(name = "minimum_service_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumServiceFee;

    // Getters and Setters

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
