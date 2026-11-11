package com.servicehub.serv.dto;

import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminBookingPaymentDto(
        UUID paymentId,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        BigDecimal amount,
        String razorpayOrderId,
        String razorpayPaymentId,
        LocalDateTime createdAt,
        LocalDateTime paidAt) {
}
