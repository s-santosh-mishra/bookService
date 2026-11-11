package com.servicehub.serv.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class WorkerSettlementReversalDto {

    private final String bankReversalReference;
    private final String reversalReason;
    private final LocalDateTime verifiedAt;
    private final UUID verifiedByAdminId;
    private final String verifiedByAdminName;

    public WorkerSettlementReversalDto(
            String bankReversalReference,
            String reversalReason,
            LocalDateTime verifiedAt,
            UUID verifiedByAdminId,
            String verifiedByAdminName) {
        this.bankReversalReference = bankReversalReference;
        this.reversalReason = reversalReason;
        this.verifiedAt = verifiedAt;
        this.verifiedByAdminId = verifiedByAdminId;
        this.verifiedByAdminName = verifiedByAdminName;
    }

    public String getBankReversalReference() {
        return bankReversalReference;
    }

    public String getReversalReason() {
        return reversalReason;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public UUID getVerifiedByAdminId() {
        return verifiedByAdminId;
    }

    public String getVerifiedByAdminName() {
        return verifiedByAdminName;
    }
}
