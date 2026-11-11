package com.servicehub.serv.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class WorkerEligiblePaymentsDto {

    private final UUID workerId;
    private final String workerName;
    private final Long totalEligiblePayments;
    private final BigDecimal totalEligibleAmount;
    private final List<EligiblePaymentDto> payments;

    public WorkerEligiblePaymentsDto(
            UUID workerId,
            String workerName,
            Long totalEligiblePayments,
            BigDecimal totalEligibleAmount,
            List<EligiblePaymentDto> payments) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.totalEligiblePayments = totalEligiblePayments;
        this.totalEligibleAmount = totalEligibleAmount;
        this.payments = List.copyOf(payments);
    }

    public UUID getWorkerId() {
        return workerId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public Long getTotalEligiblePayments() {
        return totalEligiblePayments;
    }

    public BigDecimal getTotalEligibleAmount() {
        return totalEligibleAmount;
    }

    public List<EligiblePaymentDto> getPayments() {
        return payments;
    }
}
