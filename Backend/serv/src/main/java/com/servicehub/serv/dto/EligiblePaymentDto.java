package com.servicehub.serv.dto;

import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class EligiblePaymentDto {

    private final UUID paymentId;
    private final UUID bookingId;
    private final BigDecimal paymentAmount;
    private final LocalDateTime paymentDate;
    private final LocalDateTime paymentCreatedAt;
    private final PaymentMethod paymentMethod;
    private final PaymentStatus paymentStatus;
    private final BookingStatus bookingStatus;

    public EligiblePaymentDto(
            UUID paymentId,
            UUID bookingId,
            BigDecimal paymentAmount,
            LocalDateTime paymentDate,
            LocalDateTime paymentCreatedAt,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            BookingStatus bookingStatus) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.paymentAmount = paymentAmount;
        this.paymentDate = paymentDate;
        this.paymentCreatedAt = paymentCreatedAt;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.bookingStatus = bookingStatus;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public LocalDateTime getPaymentCreatedAt() {
        return paymentCreatedAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }
}
