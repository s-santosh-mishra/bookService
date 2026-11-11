package com.servicehub.serv.repository;

import com.servicehub.serv.entity.WorkerSettlementReversal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkerSettlementReversalRepository
        extends JpaRepository<WorkerSettlementReversal, UUID> {

    boolean existsBySettlement_SettlementId(UUID settlementId);
}
