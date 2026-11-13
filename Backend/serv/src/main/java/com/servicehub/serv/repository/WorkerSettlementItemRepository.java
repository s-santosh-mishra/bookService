package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.entity.WorkerSettlementItem;
import com.servicehub.serv.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WorkerSettlementItemRepository extends JpaRepository<WorkerSettlementItem, UUID> {

    boolean existsByPayment_PaymentIdAndSettlement_StatusIn(
            UUID paymentId,
            Collection<SettlementStatus> statuses);

    @Query("""
            SELECT DISTINCT item.payment
            FROM WorkerSettlementItem item
            WHERE item.settlement.status IN :statuses
            """)
    List<Payment> findPaymentsBySettlementStatusIn(
            @Param("statuses") Collection<SettlementStatus> statuses);

    @EntityGraph(attributePaths = {
            "payment",
            "payment.booking",
            "payment.booking.worker",
            "payment.booking.worker.user",
            "settlement",
            "settlement.settledBy",
            "settlement.reversal",
            "settlement.reversal.verifiedBy"
    })
    @Query("""
            SELECT item
            FROM WorkerSettlementItem item
            ORDER BY item.payment.paymentId,
                     item.settlement.createdAt,
                     item.settlement.settlementId
            """)
    List<WorkerSettlementItem> findAllForBatchHistory();

    @EntityGraph(attributePaths = {
            "payment",
            "settlement",
            "settlement.reversal"
    })
    List<WorkerSettlementItem> findByPayment_PaymentIdIn(Collection<UUID> paymentIds);
}
