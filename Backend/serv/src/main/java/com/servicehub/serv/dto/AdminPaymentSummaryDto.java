package com.servicehub.serv.dto;

import java.math.BigDecimal;

public record AdminPaymentSummaryDto(
        long successfulCount,
        long pendingCount,
        long failedCount,
        long cancelledCount,
        long refundedCount,
        BigDecimal successfullyCollectedAmount) {
}
