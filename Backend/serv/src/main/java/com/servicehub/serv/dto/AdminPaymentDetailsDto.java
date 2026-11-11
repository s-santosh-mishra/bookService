package com.servicehub.serv.dto;

import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminPaymentDetailsDto(
        UUID paymentId,
        UUID bookingId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        String razorpayOrderId,
        String razorpayPaymentId,
        PersonInfo customer,
        PersonInfo worker,
        BookingInfo booking,
        List<AdminPaymentAttemptDto> relatedAttempts) {

    public record PersonInfo(
            String name,
            String phone,
            String email) {
    }

    public record BookingInfo(
            BookingStatus status,
            String serviceName,
            String categoryName,
            LocalDateTime createdAt) {
    }
}
