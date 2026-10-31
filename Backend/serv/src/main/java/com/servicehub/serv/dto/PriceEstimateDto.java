
package com.servicehub.serv.dto;

import java.math.BigDecimal;

public class PriceEstimateDto {

    private BigDecimal basePricePerHour;
    private BigDecimal minimumServiceFee;
    private BigDecimal estimatedLabourCharge;
    private BigDecimal estimatedTravelCharge;
    private BigDecimal estimatedTotal;
    private BigDecimal estimatedDurationHours;
    private BigDecimal estimatedDistanceKm;

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

    public BigDecimal getEstimatedLabourCharge() {
        return estimatedLabourCharge;
    }

    public void setEstimatedLabourCharge(BigDecimal estimatedLabourCharge) {
        this.estimatedLabourCharge = estimatedLabourCharge;
    }

    public BigDecimal getEstimatedTravelCharge() {
        return estimatedTravelCharge;
    }

    public void setEstimatedTravelCharge(BigDecimal estimatedTravelCharge) {
        this.estimatedTravelCharge = estimatedTravelCharge;
    }

    public BigDecimal getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(BigDecimal estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public BigDecimal getEstimatedDurationHours() {
        return estimatedDurationHours;
    }

    public void setEstimatedDurationHours(BigDecimal estimatedDurationHours) {
        this.estimatedDurationHours = estimatedDurationHours;
    }

    public BigDecimal getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public void setEstimatedDistanceKm(BigDecimal estimatedDistanceKm) {
        this.estimatedDistanceKm = estimatedDistanceKm;
    }
}
