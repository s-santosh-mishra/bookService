package com.servicehub.serv.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminBookingDetailsDto(
        AdminBookingSummaryDto booking,
        String customerEmail,
        Address customerAddress,
        String workerEmail,
        BigDecimal serviceLatitude,
        BigDecimal serviceLongitude,
        String customerNote,
        Lifecycle lifecycle,
        Billing billing,
        String adminCancellationReason,
        String workerCancellationReason,
        String workerCancellationMessage,
        List<AdminBookingPaymentDto> payments) {

    public record Address(
            String addressLine1,
            String addressLine2,
            String landmark,
            String city,
            String state,
            String pinCode) {
    }

    public record Lifecycle(
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime acceptedAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime workerCompletedAt,
            LocalDateTime cancelledAt,
            LocalDateTime failedAt,
            LocalDateTime workerCancelledAt,
            LocalDateTime completionOtpRequestedAt) {
    }

    public record Billing(
            BigDecimal basePricePerHour,
            BigDecimal minimumServiceFee,
            BigDecimal labourCharge,
            BigDecimal travelCharge,
            BigDecimal partsCost,
            BigDecimal additionalFees,
            BigDecimal finalBilledPrice,
            BigDecimal billableHours) {
    }
}
