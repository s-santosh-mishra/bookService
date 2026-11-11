package com.servicehub.serv.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class WorkerSettlementSummaryDto {

    private final UUID workerId;
    private final String workerName;
    private final Long eligiblePaymentCount;
    private final BigDecimal eligibleAmount;
    private final String bankAccountHolderName;
    private final String maskedBankAccountNumber;
    private final String bankIfscCode;
    private final String bankName;

    public WorkerSettlementSummaryDto(
            UUID workerId,
            String workerName,
            Long eligiblePaymentCount,
            BigDecimal eligibleAmount,
            String bankAccountHolderName,
            String bankAccountNumber,
            String bankIfscCode,
            String bankName) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.eligiblePaymentCount = eligiblePaymentCount;
        this.eligibleAmount = eligibleAmount;
        this.bankAccountHolderName = bankAccountHolderName;
        this.maskedBankAccountNumber = BankAccountMasker.mask(bankAccountNumber);
        this.bankIfscCode = bankIfscCode;
        this.bankName = bankName;
    }

    public UUID getWorkerId() {
        return workerId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public Long getEligiblePaymentCount() {
        return eligiblePaymentCount;
    }

    public BigDecimal getEligibleAmount() {
        return eligibleAmount;
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
}
