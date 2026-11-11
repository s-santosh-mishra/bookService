package com.servicehub.serv.dto;

import com.servicehub.serv.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class WorkerSettlementHistoryDto {

    private final UUID settlementId;
    private final LocalDate businessDate;
    private final UUID workerId;
    private final String workerName;
    private final BigDecimal amount;
    private final SettlementStatus status;
    private final Long paymentCount;
    private final String paymentReference;
    private final LocalDateTime createdAt;
    private final LocalDateTime settledAt;

    public WorkerSettlementHistoryDto(
            UUID settlementId,
            LocalDate businessDate,
            UUID workerId,
            String workerName,
            BigDecimal amount,
            SettlementStatus status,
            Long paymentCount,
            String paymentReference,
            LocalDateTime createdAt,
            LocalDateTime settledAt) {
        this.settlementId = settlementId;
        this.businessDate = businessDate;
        this.workerId = workerId;
        this.workerName = workerName;
        this.amount = amount;
        this.status = status;
        this.paymentCount = paymentCount;
        this.paymentReference = paymentReference;
        this.createdAt = createdAt;
        this.settledAt = settledAt;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public UUID getWorkerId() {
        return workerId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public Long getPaymentCount() {
        return paymentCount;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }
}
