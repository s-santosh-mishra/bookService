package com.servicehub.serv.dto;

import java.math.BigDecimal;
import java.util.List;

public class WorkerSettlementBatchesDto {

    private final List<WorkerSettlementBatchDto> batches;
    private final long unclassifiedSuccessfulPaymentCount;
    private final BigDecimal unclassifiedSuccessfulPaymentAmount;
    private final long unclassifiedSettlementCount;

    public WorkerSettlementBatchesDto(
            List<WorkerSettlementBatchDto> batches,
            long unclassifiedSuccessfulPaymentCount,
            BigDecimal unclassifiedSuccessfulPaymentAmount,
            long unclassifiedSettlementCount) {
        this.batches = List.copyOf(batches);
        this.unclassifiedSuccessfulPaymentCount = unclassifiedSuccessfulPaymentCount;
        this.unclassifiedSuccessfulPaymentAmount = unclassifiedSuccessfulPaymentAmount;
        this.unclassifiedSettlementCount = unclassifiedSettlementCount;
    }

    public List<WorkerSettlementBatchDto> getBatches() {
        return batches;
    }

    public long getUnclassifiedSuccessfulPaymentCount() {
        return unclassifiedSuccessfulPaymentCount;
    }

    public BigDecimal getUnclassifiedSuccessfulPaymentAmount() {
        return unclassifiedSuccessfulPaymentAmount;
    }

    public long getUnclassifiedSettlementCount() {
        return unclassifiedSettlementCount;
    }
}
