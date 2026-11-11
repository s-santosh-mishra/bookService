package com.servicehub.serv.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "worker_settlement_reversals")
public class WorkerSettlementReversal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reversal_id", nullable = false, updatable = false)
    private UUID reversalId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "settlement_id",
            nullable = false,
            unique = true,
            updatable = false)
    private WorkerSettlement settlement;

    @Column(name = "bank_reversal_reference", nullable = false, length = 100, updatable = false)
    private String bankReversalReference;

    @Column(name = "reversal_reason", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String reversalReason;

    @Column(name = "verified_at", nullable = false, updatable = false)
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verified_by", nullable = false, updatable = false)
    private Users verifiedBy;

    protected WorkerSettlementReversal() {
    }

    public WorkerSettlementReversal(
            WorkerSettlement settlement,
            String bankReversalReference,
            String reversalReason,
            LocalDateTime verifiedAt,
            Users verifiedBy) {
        this.settlement = settlement;
        this.bankReversalReference = bankReversalReference;
        this.reversalReason = reversalReason;
        this.verifiedAt = verifiedAt;
        this.verifiedBy = verifiedBy;
    }

    public UUID getReversalId() {
        return reversalId;
    }

    public WorkerSettlement getSettlement() {
        return settlement;
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

    public Users getVerifiedBy() {
        return verifiedBy;
    }
}
