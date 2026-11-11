package com.servicehub.serv.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CreateSettlementRequestDto {

    @NotNull
    private UUID workerId;

    @NotEmpty
    private List<@NotNull UUID> paymentIds;

    private LocalDate businessDate;

    public UUID getWorkerId() {
        return workerId;
    }

    public void setWorkerId(UUID workerId) {
        this.workerId = workerId;
    }

    public List<UUID> getPaymentIds() {
        return paymentIds;
    }

    public void setPaymentIds(List<UUID> paymentIds) {
        this.paymentIds = paymentIds;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }
}
