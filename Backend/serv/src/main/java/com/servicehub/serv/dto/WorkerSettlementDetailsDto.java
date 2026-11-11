package com.servicehub.serv.dto;

import com.servicehub.serv.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class WorkerSettlementDetailsDto {

    private final UUID settlementId;
    private final LocalDate businessDate;
    private final UUID workerId;
    private final String workerName;
    private final BigDecimal amount;
    private final BigDecimal paymentItemsTotal;
    private final boolean paymentItemsTotalMatchesAmount;
    private final SettlementStatus status;
    private final String paymentReference;
    private final LocalDateTime createdAt;
    private final LocalDateTime settledAt;
    private final String failureReason;
    private final String bankAccountHolderName;
    private final String maskedBankAccountNumber;
    private final String bankIfscCode;
    private final String bankName;
    private final UUID createdByAdminId;
    private final String createdByAdminName;
    private final List<WorkerSettlementItemDto> paymentItems;
    private final WorkerSettlementReversalDto reversal;

    public WorkerSettlementDetailsDto(
            UUID settlementId,
            LocalDate businessDate,
            UUID workerId,
            String workerName,
            BigDecimal amount,
            BigDecimal paymentItemsTotal,
            boolean paymentItemsTotalMatchesAmount,
            SettlementStatus status,
            String paymentReference,
            LocalDateTime createdAt,
            LocalDateTime settledAt,
            String failureReason,
            String bankAccountHolderName,
            String bankAccountNumber,
            String bankIfscCode,
            String bankName,
            UUID createdByAdminId,
            String createdByAdminName,
            List<WorkerSettlementItemDto> paymentItems,
            WorkerSettlementReversalDto reversal) {
        this.settlementId = settlementId;
        this.businessDate = businessDate;
        this.workerId = workerId;
        this.workerName = workerName;
        this.amount = amount;
        this.paymentItemsTotal = paymentItemsTotal;
        this.paymentItemsTotalMatchesAmount = paymentItemsTotalMatchesAmount;
        this.status = status;
        this.paymentReference = paymentReference;
        this.createdAt = createdAt;
        this.settledAt = settledAt;
        this.failureReason = failureReason;
        this.bankAccountHolderName = bankAccountHolderName;
        this.maskedBankAccountNumber = BankAccountMasker.mask(bankAccountNumber);
        this.bankIfscCode = bankIfscCode;
        this.bankName = bankName;
        this.createdByAdminId = createdByAdminId;
        this.createdByAdminName = createdByAdminName;
        this.paymentItems = List.copyOf(paymentItems);
        this.reversal = reversal;
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

    public BigDecimal getPaymentItemsTotal() {
        return paymentItemsTotal;
    }

    public boolean isPaymentItemsTotalMatchesAmount() {
        return paymentItemsTotalMatchesAmount;
    }

    public SettlementStatus getStatus() {
        return status;
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

    public String getFailureReason() {
        return failureReason;
    }

    public String getBankAccountHolderName() {
        return bankAccountHolderName;
    }

    public String getMaskedBankAccountNumber() {
        return maskedBankAccountNumber;
    }

    public String getBankIfscCode() {
        return bankIfscCode;
    }

    public String getBankName() {
        return bankName;
    }

    public UUID getCreatedByAdminId() {
        return createdByAdminId;
    }

    public String getCreatedByAdminName() {
        return createdByAdminName;
    }

    public List<WorkerSettlementItemDto> getPaymentItems() {
        return paymentItems;
    }

    public WorkerSettlementReversalDto getReversal() {
        return reversal;
    }
}
