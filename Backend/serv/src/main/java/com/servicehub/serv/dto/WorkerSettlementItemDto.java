package com.servicehub.serv.dto;

import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class WorkerSettlementItemDto {

    private final UUID paymentId;
    private final UUID bookingId;
    private final BigDecimal amount;
    private final LocalDateTime paymentDate;
    private final LocalDateTime paymentCreatedAt;
    private final PaymentMethod paymentMethod;
    private final PaymentStatus paymentStatus;

    public WorkerSettlementItemDto(
            UUID paymentId,
            UUID bookingId,
            BigDecimal amount,
            LocalDateTime paymentDate,
            LocalDateTime paymentCreatedAt,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentCreatedAt = paymentCreatedAt;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public BigDecimal getAmount() {
        return amount;
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
}
