package com.servicehub.serv.repository;

import com.servicehub.serv.entity.WorkerSettlement;
import com.servicehub.serv.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkerSettlementRepository extends JpaRepository<WorkerSettlement, UUID> {

    List<WorkerSettlement> findByWorker_UserIdOrderByCreatedAtDesc(UUID workerId);

    List<WorkerSettlement> findByStatusOrderByCreatedAtDesc(SettlementStatus status);

    long countByStatus(SettlementStatus status);

    @Query("""
            SELECT SUM(settlement.amount)
            FROM WorkerSettlement settlement
            WHERE settlement.status = :status
            """)
    BigDecimal sumAmountByStatus(@Param("status") SettlementStatus status);

    @Query("""
            SELECT new com.servicehub.serv.dto.WorkerSettlementHistoryDto(
                settlement.settlementId,
                settlement.businessDate,
                worker.userId,
                worker.user.fullName,
                settlement.amount,
                settlement.status,
                COUNT(item.settlementItemId),
                settlement.paymentReference,
                settlement.createdAt,
                settlement.settledAt
            )
            FROM WorkerSettlement settlement
            JOIN settlement.worker worker
            LEFT JOIN settlement.items item
            WHERE (:workerId IS NULL OR worker.userId = :workerId)
              AND (:status IS NULL OR settlement.status = :status)
            GROUP BY
                settlement.settlementId,
                worker.userId,
                worker.user.fullName,
                settlement.amount,
                settlement.status,
                settlement.paymentReference,
                settlement.createdAt,
                settlement.settledAt
            ORDER BY settlement.createdAt DESC, settlement.settlementId DESC
            """)
    List<com.servicehub.serv.dto.WorkerSettlementHistoryDto> findSettlementHistory(
            @Param("workerId") UUID workerId,
            @Param("status") SettlementStatus status);

    @EntityGraph(attributePaths = {
            "worker",
            "worker.user",
            "settledBy",
            "items",
            "items.payment",
            "items.payment.booking",
            "reversal",
            "reversal.verifiedBy"
    })
    @Query("""
            SELECT DISTINCT settlement
            FROM WorkerSettlement settlement
            WHERE settlement.settlementId = :settlementId
            """)
    Optional<WorkerSettlement> findDetailsBySettlementId(
            @Param("settlementId") UUID settlementId);

    @EntityGraph(attributePaths = {
            "worker",
            "worker.user",
            "settledBy",
            "reversal",
            "reversal.verifiedBy"
    })
    @Query("""
            SELECT settlement
            FROM WorkerSettlement settlement
            ORDER BY settlement.createdAt, settlement.settlementId
            """)
    List<WorkerSettlement> findAllForBatchDashboard();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT settlement
            FROM WorkerSettlement settlement
            WHERE settlement.settlementId = :settlementId
            """)
    Optional<WorkerSettlement> findBySettlementIdForUpdate(
            @Param("settlementId") UUID settlementId);
}
