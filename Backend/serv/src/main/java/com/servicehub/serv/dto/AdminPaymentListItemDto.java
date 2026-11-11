package com.servicehub.serv.dto;

import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminPaymentListItemDto(
        UUID paymentId,
        UUID bookingId,
        String customerName,
        String customerPhone,
        String workerName,
        String workerPhone,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime paidAt) {
}
