package com.servicehub.serv.dto;

import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminBookingSummaryDto(
        UUID bookingId,
        UUID customerId,
        String customerName,
        String customerPhone,
        UUID workerId,
        String workerName,
        String workerPhone,
        UUID serviceId,
        String serviceName,
        String categoryName,
        BookingStatus status,
        LocalDateTime createdAt,
        BigDecimal finalBilledPrice,
        List<PaymentMethod> paymentMethods,
        List<PaymentStatus> paymentStatuses) {
}
