package com.servicehub.serv.dto;

import com.servicehub.serv.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class WorkerSettlementResponseDto {

    private final UUID settlementId;
    private final LocalDate businessDate;
    private final UUID workerId;
    private final String workerName;
    private final BigDecimal amount;
    private final SettlementStatus status;
    private final int paymentCount;
    private final String bankAccountHolderName;
    private final String maskedBankAccountNumber;
    private final String bankIfscCode;
    private final String bankName;
    private final String paymentReference;
    private final String failureReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime settledAt;

    public WorkerSettlementResponseDto(
            UUID settlementId,
            LocalDate businessDate,
            UUID workerId,
            String workerName,
            BigDecimal amount,
            SettlementStatus status,
            int paymentCount,
            String bankAccountHolderName,
            String bankAccountNumber,
            String bankIfscCode,
            String bankName,
            String paymentReference,
            String failureReason,
            LocalDateTime createdAt,
            LocalDateTime settledAt) {
        this.settlementId = settlementId;
        this.businessDate = businessDate;
        this.workerId = workerId;
        this.workerName = workerName;
        this.amount = amount;
        this.status = status;
        this.paymentCount = paymentCount;
        this.bankAccountHolderName = bankAccountHolderName;
        this.maskedBankAccountNumber = BankAccountMasker.mask(bankAccountNumber);
        this.bankIfscCode = bankIfscCode;
        this.bankName = bankName;
        this.paymentReference = paymentReference;
        this.failureReason = failureReason;
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

    public int getPaymentCount() {
        return paymentCount;
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

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }
}
