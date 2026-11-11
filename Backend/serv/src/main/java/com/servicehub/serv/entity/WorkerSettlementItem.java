package com.servicehub.serv.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "worker_settlement_items",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"settlement_id", "payment_id"}))
public class WorkerSettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "settlement_item_id", nullable = false, updatable = false)
    private UUID settlementItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private WorkerSettlement settlement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    public UUID getSettlementItemId() {
        return settlementItemId;
    }

    public void setSettlementItemId(UUID settlementItemId) {
        this.settlementItemId = settlementItemId;
    }

    public WorkerSettlement getSettlement() {
        return settlement;
    }

    public void setSettlement(WorkerSettlement settlement) {
        this.settlement = settlement;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
