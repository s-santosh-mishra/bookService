
package com.servicehub.serv.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PriceEstimateRequestDto {

    private UUID serviceId;
    private BigDecimal customerLatitude;
    private BigDecimal customerLongitude;
    private BigDecimal estimatedDurationHours;

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public BigDecimal getCustomerLatitude() {
        return customerLatitude;
    }

    public void setCustomerLatitude(BigDecimal customerLatitude) {
        this.customerLatitude = customerLatitude;
    }

    public BigDecimal getCustomerLongitude() {
        return customerLongitude;
    }

    public void setCustomerLongitude(BigDecimal customerLongitude) {
        this.customerLongitude = customerLongitude;
    }

    public BigDecimal getEstimatedDurationHours() {
        return estimatedDurationHours;
    }

    public void setEstimatedDurationHours(BigDecimal estimatedDurationHours) {
        this.estimatedDurationHours = estimatedDurationHours;
    }
}
