package com.servicehub.serv.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "billings")
public class Billing {

    @Id
    @GeneratedValue
    @Column(name = "billing_id")
    private UUID billingId;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "base_price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePricePerHour;

    @Column(name = "minimum_service_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumServiceFee;

    @Column(name = "labour_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal labourCharge;

    @Column(name = "travel_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal travelCharge = BigDecimal.ZERO;

    @Column(name = "parts_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal partsCost = BigDecimal.ZERO;

    @Column(name = "additional_fees", nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalFees = BigDecimal.ZERO;

    @Column(name = "final_billed_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalBilledPrice;

    @Column(name = "worker_rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal workerRating;

    @Column(name = "billable_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal billableHours;

    // Getters and Setters

    public UUID getBillingId() {
        return billingId;
    }

    public void setBillingId(UUID billingId) {
        this.billingId = billingId;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
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

    public BigDecimal getLabourCharge() {
        return labourCharge;
    }

    public void setLabourCharge(BigDecimal labourCharge) {
        this.labourCharge = labourCharge;
    }

    public BigDecimal getTravelCharge() {
        return travelCharge;
    }

    public void setTravelCharge(BigDecimal travelCharge) {
        this.travelCharge = travelCharge;
    }

    public BigDecimal getPartsCost() {
        return partsCost;
    }

    public void setPartsCost(BigDecimal partsCost) {
        this.partsCost = partsCost;
    }

    public BigDecimal getAdditionalFees() {
        return additionalFees;
    }

    public void setAdditionalFees(BigDecimal additionalFees) {
        this.additionalFees = additionalFees;
    }

    public BigDecimal getFinalBilledPrice() {
        return finalBilledPrice;
    }

    public void setFinalBilledPrice(BigDecimal finalBilledPrice) {
        this.finalBilledPrice = finalBilledPrice;
    }

    public BigDecimal getWorkerRating() {
        return workerRating;
    }

    public void setWorkerRating(BigDecimal workerRating) {
        this.workerRating = workerRating;
    }

    public BigDecimal getBillableHours() {
        return billableHours;
    }

    public void setBillableHours(BigDecimal billableHours) {
        this.billableHours = billableHours;
    }
}