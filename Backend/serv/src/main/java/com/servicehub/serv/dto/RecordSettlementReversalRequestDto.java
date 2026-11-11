package com.servicehub.serv.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RecordSettlementReversalRequestDto {

    @NotBlank
    @Size(max = 100)
    private String bankReversalReference;

    @NotBlank
    @Size(max = 1000)
    private String reversalReason;

    @NotNull
    @AssertTrue(message = "Confirm verification against external bank records.")
    private Boolean verifiedWithBankRecords;

    public String getBankReversalReference() {
        return bankReversalReference;
    }

    public void setBankReversalReference(String bankReversalReference) {
        this.bankReversalReference = bankReversalReference;
    }

    public String getReversalReason() {
        return reversalReason;
    }

    public void setReversalReason(String reversalReason) {
        this.reversalReason = reversalReason;
    }

    public Boolean getVerifiedWithBankRecords() {
        return verifiedWithBankRecords;
    }

    public void setVerifiedWithBankRecords(Boolean verifiedWithBankRecords) {
        this.verifiedWithBankRecords = verifiedWithBankRecords;
    }
}
