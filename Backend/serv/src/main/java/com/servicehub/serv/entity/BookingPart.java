package com.servicehub.serv.entity;

import com.servicehub.serv.enums.PartApprovalStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_parts")
public class BookingPart {

    @Id
    @GeneratedValue
    @Column(name = "booking_part_id")
    private UUID bookingPartId;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "part_name", nullable = false, length = 150)
    private String partName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "photo_url", nullable = false, length = 1000)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PartApprovalStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public UUID getBookingPartId() {
        return bookingPartId;
    }

    public void setBookingPartId(UUID bookingPartId) {
        this.bookingPartId = bookingPartId;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
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