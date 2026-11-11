
package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.math.BigDecimal;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    String ELIGIBLE_SETTLEMENT_PAYMENT_PREDICATE = """
            payment.status = com.servicehub.serv.enums.PaymentStatus.SUCCESS
              AND payment.paymentMethod = com.servicehub.serv.enums.PaymentMethod.RAZORPAY
              AND payment.paidAt IS NOT NULL
              AND payment.booking.status IN (
                  com.servicehub.serv.enums.BookingStatus.COMPLETED,
                  com.servicehub.serv.enums.BookingStatus.REVIEWED
              )
              AND NOT EXISTS (
                  SELECT activeItem
                  FROM WorkerSettlementItem activeItem
                  WHERE activeItem.payment = payment
                    AND activeItem.settlement.status IN (
                        com.servicehub.serv.enums.SettlementStatus.PENDING,
                        com.servicehub.serv.enums.SettlementStatus.PROCESSING
                    )
              )
              AND NOT EXISTS (
                  SELECT settledItem
                  FROM WorkerSettlementItem settledItem
                  WHERE settledItem.payment = payment
                    AND settledItem.settlement.status =
                        com.servicehub.serv.enums.SettlementStatus.SETTLED
                    AND NOT EXISTS (
                        SELECT reversal
                        FROM WorkerSettlementReversal reversal
                        WHERE reversal.settlement = settledItem.settlement
                    )
              )
            """;

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByBookingBookingIdAndPaymentMethodAndStatus(
            UUID bookingId,
            PaymentMethod paymentMethod,
            PaymentStatus status
    );

    List<Payment> findByBookingBookingIdAndStatus(
            UUID bookingId,
            PaymentStatus status
    );

    Optional<Payment> findByBookingBookingIdAndPaymentMethodAndStatusAndRazorpayOrderIdIsNotNull(
            UUID bookingId,
            PaymentMethod paymentMethod,
            PaymentStatus status
    );

    boolean existsByBookingBookingIdAndStatus(
            UUID bookingId,
            PaymentStatus status
    );

    Optional<Payment> findFirstByBookingBookingIdOrderByCreatedAtDesc(
        UUID bookingId);

    List<Payment> findByBookingBookingIdInOrderByCreatedAtDesc(
            Collection<UUID> bookingIds);

    @EntityGraph(attributePaths = {
            "booking.customer.user",
            "booking.worker.user"
    })
    @Query("SELECT payment FROM Payment payment "
            + "ORDER BY payment.createdAt DESC")
    List<Payment> findAllForAdmin();

    @EntityGraph(attributePaths = {
            "booking.customer.user.credentials",
            "booking.worker.user.credentials",
            "booking.service.category"
    })
    Optional<Payment> findByPaymentId(UUID paymentId);

    List<Payment> findByBookingBookingIdOrderByCreatedAtDesc(UUID bookingId);

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT payment FROM Payment payment WHERE "
            + ELIGIBLE_SETTLEMENT_PAYMENT_PREDICATE
            + " AND payment.booking.worker.userId = :workerId"
            + " AND payment.paymentId IN :paymentIds"
            + " ORDER BY payment.paymentId")
    List<Payment> findEligiblePaymentsForWorkerSettlementForUpdate(
            @Param("workerId") UUID workerId,
            @Param("paymentIds") Collection<UUID> paymentIds);

    @Query("SELECT payment FROM Payment payment WHERE "
            + ELIGIBLE_SETTLEMENT_PAYMENT_PREDICATE
            + " ORDER BY payment.paidAt, payment.paymentId")
    List<Payment> findEligiblePaymentsForSettlement();

    @EntityGraph(attributePaths = {
            "booking",
            "booking.worker",
            "booking.worker.user"
    })
    @Query("""
            SELECT payment
            FROM Payment payment
            WHERE payment.status = com.servicehub.serv.enums.PaymentStatus.SUCCESS
              AND payment.paymentMethod = com.servicehub.serv.enums.PaymentMethod.RAZORPAY
              AND payment.paidAt IS NOT NULL
              AND payment.booking.status IN (
                  com.servicehub.serv.enums.BookingStatus.COMPLETED,
                  com.servicehub.serv.enums.BookingStatus.REVIEWED
              )
            ORDER BY payment.paidAt, payment.paymentId
            """)
    List<Payment> findSettlementBatchPayments();

    @Query("""
            SELECT COUNT(payment.paymentId)
            FROM Payment payment
            WHERE payment.status = com.servicehub.serv.enums.PaymentStatus.SUCCESS
              AND payment.paymentMethod = com.servicehub.serv.enums.PaymentMethod.RAZORPAY
              AND payment.paidAt IS NULL
              AND payment.booking.status IN (
                  com.servicehub.serv.enums.BookingStatus.COMPLETED,
                  com.servicehub.serv.enums.BookingStatus.REVIEWED
              )
            """)
    long countUndatedSuccessfulOnlinePayments();

    @Query("""
            SELECT COALESCE(SUM(payment.amount), 0)
            FROM Payment payment
            WHERE payment.status = com.servicehub.serv.enums.PaymentStatus.SUCCESS
              AND payment.paymentMethod = com.servicehub.serv.enums.PaymentMethod.RAZORPAY
              AND payment.paidAt IS NULL
              AND payment.booking.status IN (
                  com.servicehub.serv.enums.BookingStatus.COMPLETED,
                  com.servicehub.serv.enums.BookingStatus.REVIEWED
              )
            """)
    BigDecimal sumUndatedSuccessfulOnlinePaymentAmount();

    @Query("SELECT payment FROM Payment payment JOIN FETCH payment.booking WHERE "
            + ELIGIBLE_SETTLEMENT_PAYMENT_PREDICATE
            + " AND payment.booking.worker.userId = :workerId")
    List<Payment> findEligiblePaymentsForWorker(
            @Param("workerId") UUID workerId);

    @Query("""
            SELECT new com.servicehub.serv.dto.WorkerSettlementSummaryDto(
                worker.userId,
                worker.user.fullName,
                COUNT(payment.paymentId),
                SUM(payment.amount),
                worker.bankAccountHolderName,
                worker.bankAccountNumber,
                worker.bankIfscCode,
                worker.bankName
            )
            FROM Payment payment
            JOIN payment.booking booking
            JOIN booking.worker worker
            WHERE
            """ + ELIGIBLE_SETTLEMENT_PAYMENT_PREDICATE + """
            GROUP BY
                worker.userId,
                worker.user.fullName,
                worker.bankAccountHolderName,
                worker.bankAccountNumber,
                worker.bankIfscCode,
                worker.bankName
            ORDER BY worker.user.fullName
            """)
    List<com.servicehub.serv.dto.WorkerSettlementSummaryDto>
            findWorkersWithEligibleSettlementPayments();
}
