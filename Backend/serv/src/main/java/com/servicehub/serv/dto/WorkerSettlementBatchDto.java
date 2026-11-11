package com.servicehub.serv.dto;

import com.servicehub.serv.enums.SettlementBatchStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class WorkerSettlementBatchDto {

    private final LocalDate businessDate;
    private final SettlementBatchStatus status;
    private final boolean closedForSettlement;
    private final long batchPaymentCount;
    private final BigDecimal batchPaymentAmount;
    private final long eligiblePaymentCount;
    private final BigDecimal eligibleAmount;
    private final long eligibleWorkerCount;
    private final long settlementsCreatedCount;
    private final BigDecimal successfullySettledAmount;
    private final BigDecimal failedSettlementAmount;
    private final BigDecimal processingAmount;
    private final BigDecimal pendingAmount;
    private final long remainingEligiblePaymentCount;
    private final BigDecimal remainingEligibleAmount;
    private final List<WorkerSummary> workers;

    public WorkerSettlementBatchDto(
            LocalDate businessDate,
            SettlementBatchStatus status,
            boolean closedForSettlement,
            long batchPaymentCount,
            BigDecimal batchPaymentAmount,
            long eligiblePaymentCount,
            BigDecimal eligibleAmount,
            long eligibleWorkerCount,
            long settlementsCreatedCount,
            BigDecimal successfullySettledAmount,
            BigDecimal failedSettlementAmount,
            BigDecimal processingAmount,
            BigDecimal pendingAmount,
            long remainingEligiblePaymentCount,
            BigDecimal remainingEligibleAmount,
            List<WorkerSummary> workers) {
        this.businessDate = businessDate;
        this.status = status;
        this.closedForSettlement = closedForSettlement;
        this.batchPaymentCount = batchPaymentCount;
        this.batchPaymentAmount = batchPaymentAmount;
        this.eligiblePaymentCount = eligiblePaymentCount;
        this.eligibleAmount = eligibleAmount;
        this.eligibleWorkerCount = eligibleWorkerCount;
        this.settlementsCreatedCount = settlementsCreatedCount;
        this.successfullySettledAmount = successfullySettledAmount;
        this.failedSettlementAmount = failedSettlementAmount;
        this.processingAmount = processingAmount;
        this.pendingAmount = pendingAmount;
        this.remainingEligiblePaymentCount = remainingEligiblePaymentCount;
        this.remainingEligibleAmount = remainingEligibleAmount;
        this.workers = List.copyOf(workers);
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public SettlementBatchStatus getStatus() {
        return status;
    }

    public boolean isClosedForSettlement() {
        return closedForSettlement;
    }

    public long getBatchPaymentCount() {
        return batchPaymentCount;
    }

    public BigDecimal getBatchPaymentAmount() {
        return batchPaymentAmount;
    }

    public long getEligiblePaymentCount() {
        return eligiblePaymentCount;
    }

    public BigDecimal getEligibleAmount() {
        return eligibleAmount;
    }

    public long getEligibleWorkerCount() {
        return eligibleWorkerCount;
    }

    public long getSettlementsCreatedCount() {
        return settlementsCreatedCount;
    }

    public BigDecimal getSuccessfullySettledAmount() {
        return successfullySettledAmount;
    }

    public BigDecimal getFailedSettlementAmount() {
        return failedSettlementAmount;
    }

    public BigDecimal getProcessingAmount() {
        return processingAmount;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public long getRemainingEligiblePaymentCount() {
        return remainingEligiblePaymentCount;
    }

    public BigDecimal getRemainingEligibleAmount() {
        return remainingEligibleAmount;
    }

    public List<WorkerSummary> getWorkers() {
        return workers;
    }

    public static class WorkerSummary {

        private final UUID workerId;
        private final String workerName;
        private final String bankAccountHolderName;
        private final String maskedBankAccountNumber;
        private final String bankIfscCode;
        private final String bankName;
        private final long eligiblePaymentCount;
        private final BigDecimal eligibleAmount;

        public WorkerSummary(
                UUID workerId,
                String workerName,
                String bankAccountHolderName,
                String maskedBankAccountNumber,
                String bankIfscCode,
                String bankName,
                long eligiblePaymentCount,
                BigDecimal eligibleAmount) {
            this.workerId = workerId;
            this.workerName = workerName;
            this.bankAccountHolderName = bankAccountHolderName;
            this.maskedBankAccountNumber = maskedBankAccountNumber;
            this.bankIfscCode = bankIfscCode;
            this.bankName = bankName;
            this.eligiblePaymentCount = eligiblePaymentCount;
            this.eligibleAmount = eligibleAmount;
        }

        public UUID getWorkerId() {
            return workerId;
        }

        public String getWorkerName() {
            return workerName;
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

        public long getEligiblePaymentCount() {
            return eligiblePaymentCount;
        }

        public BigDecimal getEligibleAmount() {
            return eligibleAmount;
        }
    }
}
