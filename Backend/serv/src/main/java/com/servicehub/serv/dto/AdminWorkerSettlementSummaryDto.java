package com.servicehub.serv.dto;

import java.math.BigDecimal;

public record AdminWorkerSettlementSummaryDto(
        long workersWithEligiblePayments,
        BigDecimal totalEligibleAmount,
        long pendingSettlements,
        long processingSettlements,
        BigDecimal successfullySettledAmount,
        long failedSettlements) {
}
