package com.servicehub.serv.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WorkerDailySettlementSummaryDto {

    private LocalDate businessDate;
    private int completedJobsCount;
    private BigDecimal totalEarnings;
    private BigDecimal cashEarnings;
    private BigDecimal onlineEarnings;
    private BigDecimal settledAmount;
    private BigDecimal pendingSettlementAmount;
    private BigDecimal unsettledAmount;
    private boolean batchClosed;
    private String settlementStatus;
    private String settlementStatusMessage;

    public WorkerDailySettlementSummaryDto() {
    }

    public WorkerDailySettlementSummaryDto(
            LocalDate businessDate,
            int completedJobsCount,
            BigDecimal totalEarnings,
            BigDecimal cashEarnings,
            BigDecimal onlineEarnings,
            BigDecimal settledAmount,
            BigDecimal pendingSettlementAmount,
            BigDecimal unsettledAmount,
            boolean batchClosed,
            String settlementStatus,
            String settlementStatusMessage) {
        this.businessDate = businessDate;
        this.completedJobsCount = completedJobsCount;
        this.totalEarnings = totalEarnings;
        this.cashEarnings = cashEarnings;
        this.onlineEarnings = onlineEarnings;
        this.settledAmount = settledAmount;
        this.pendingSettlementAmount = pendingSettlementAmount;
        this.unsettledAmount = unsettledAmount;
        this.batchClosed = batchClosed;
        this.settlementStatus = settlementStatus;
        this.settlementStatusMessage = settlementStatusMessage;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public int getCompletedJobsCount() {
        return completedJobsCount;
    }

    public void setCompletedJobsCount(int completedJobsCount) {
        this.completedJobsCount = completedJobsCount;
    }

    public BigDecimal getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(BigDecimal totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public BigDecimal getCashEarnings() {
        return cashEarnings;
    }

    public void setCashEarnings(BigDecimal cashEarnings) {
        this.cashEarnings = cashEarnings;
    }

    public BigDecimal getOnlineEarnings() {
        return onlineEarnings;
    }

    public void setOnlineEarnings(BigDecimal onlineEarnings) {
        this.onlineEarnings = onlineEarnings;
    }

    public BigDecimal getSettledAmount() {
        return settledAmount;
    }

    public void setSettledAmount(BigDecimal settledAmount) {
        this.settledAmount = settledAmount;
    }

    public BigDecimal getPendingSettlementAmount() {
        return pendingSettlementAmount;
    }

    public void setPendingSettlementAmount(BigDecimal pendingSettlementAmount) {
        this.pendingSettlementAmount = pendingSettlementAmount;
    }

    public BigDecimal getUnsettledAmount() {
        return unsettledAmount;
    }

    public void setUnsettledAmount(BigDecimal unsettledAmount) {
        this.unsettledAmount = unsettledAmount;
    }

    public boolean isBatchClosed() {
        return batchClosed;
    }

    public void setBatchClosed(boolean batchClosed) {
        this.batchClosed = batchClosed;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public String getSettlementStatusMessage() {
        return settlementStatusMessage;
    }

    public void setSettlementStatusMessage(String settlementStatusMessage) {
        this.settlementStatusMessage = settlementStatusMessage;
    }
}
