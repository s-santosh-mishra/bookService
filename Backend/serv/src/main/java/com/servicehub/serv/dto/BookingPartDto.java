package com.servicehub.serv.dto;

import com.servicehub.serv.enums.PartApprovalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BookingPartDto {

    private UUID bookingPartId;
    private String partName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPrice;
    private PartApprovalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public BookingPartDto() {
    }

    public BookingPartDto(
            UUID bookingPartId,
            String partName,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal totalPrice,
            PartApprovalStatus status,
            LocalDateTime createdAt,
            LocalDateTime approvedAt) {

        this.bookingPartId = bookingPartId;
        this.partName = partName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
    }

    public UUID getBookingPartId() {
        return bookingPartId;
    }

    public void setBookingPartId(UUID bookingPartId) {
        this.bookingPartId = bookingPartId;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public PartApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(PartApprovalStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}