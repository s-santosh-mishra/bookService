package com.servicehub.serv.dto;

import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;
import com.servicehub.serv.enums.SettlementBatchStatus;
import com.servicehub.serv.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class WorkerSettlementBatchPaymentsDto {

    private final LocalDate businessDate;
    private final SettlementBatchStatus status;
    private final boolean closedForSettlement;
    private final List<PaymentRow> payments;

    public WorkerSettlementBatchPaymentsDto(
            LocalDate businessDate,
            SettlementBatchStatus status,
            boolean closedForSettlement,
            List<PaymentRow> payments) {
        this.businessDate = businessDate;
        this.status = status;
        this.closedForSettlement = closedForSettlement;
        this.payments = List.copyOf(payments);
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public SettlementBatchStatus getStatus() {
        return status;
    }

    public boolean isClosedForSettlement() {
        return closedForSettlement;
    }

    public List<PaymentRow> getPayments() {
        return payments;
    }

    public static class PaymentRow {

        private final UUID paymentId;
        private final UUID workerId;
        private final String workerName;
        private final UUID bookingId;
        private final BookingStatus bookingStatus;
        private final LocalDateTime paidAt;
        private final LocalDateTime paymentCreatedAt;
        private final BigDecimal amount;
        private final PaymentMethod paymentMethod;
        private final PaymentStatus paymentStatus;
        private final boolean eligibleForSettlement;
        private final UUID blockingSettlementId;
        private final List<SettlementAttempt> settlementAttempts;

        public PaymentRow(
                UUID paymentId,
                UUID workerId,
                String workerName,
                UUID bookingId,
                BookingStatus bookingStatus,
                LocalDateTime paidAt,
                LocalDateTime paymentCreatedAt,
                BigDecimal amount,
                PaymentMethod paymentMethod,
                PaymentStatus paymentStatus,
                boolean eligibleForSettlement,
                UUID blockingSettlementId,
                List<SettlementAttempt> settlementAttempts) {
            this.paymentId = paymentId;
            this.workerId = workerId;
            this.workerName = workerName;
            this.bookingId = bookingId;
            this.bookingStatus = bookingStatus;
            this.paidAt = paidAt;
            this.paymentCreatedAt = paymentCreatedAt;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.paymentStatus = paymentStatus;
            this.eligibleForSettlement = eligibleForSettlement;
            this.blockingSettlementId = blockingSettlementId;
            this.settlementAttempts = List.copyOf(settlementAttempts);
        }

        public UUID getPaymentId() {
            return paymentId;
        }

        public UUID getWorkerId() {
            return workerId;
        }

        public String getWorkerName() {
            return workerName;
        }

        public UUID getBookingId() {
            return bookingId;
        }

        public BookingStatus getBookingStatus() {
            return bookingStatus;
        }

        public LocalDateTime getPaidAt() {
            return paidAt;
        }

        public LocalDateTime getPaymentCreatedAt() {
            return paymentCreatedAt;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public PaymentStatus getPaymentStatus() {
            return paymentStatus;
        }

        public boolean isEligibleForSettlement() {
            return eligibleForSettlement;
        }

        public UUID getBlockingSettlementId() {
            return blockingSettlementId;
        }

        public List<SettlementAttempt> getSettlementAttempts() {
            return settlementAttempts;
        }
    }

    public static class SettlementAttempt {

        private final UUID settlementId;
        private final LocalDate businessDate;
        private final SettlementStatus status;
        private final BigDecimal amount;
        private final String failureReason;
        private final String paymentReference;
        private final LocalDateTime createdAt;
        private final LocalDateTime settledAt;
        private final String reversalReference;
        private final String reversalReason;
        private final LocalDateTime reversalVerifiedAt;
        private final String reversalVerifiedByAdmin;

        public SettlementAttempt(
                UUID settlementId,
                LocalDate businessDate,
                SettlementStatus status,
                BigDecimal amount,
                String failureReason,
                String paymentReference,
                LocalDateTime createdAt,
                LocalDateTime settledAt,
                String reversalReference,
                String reversalReason,
                LocalDateTime reversalVerifiedAt,
                String reversalVerifiedByAdmin) {
            this.settlementId = settlementId;
            this.businessDate = businessDate;
            this.status = status;
            this.amount = amount;
            this.failureReason = failureReason;
            this.paymentReference = paymentReference;
            this.createdAt = createdAt;
            this.settledAt = settledAt;
            this.reversalReference = reversalReference;
            this.reversalReason = reversalReason;
            this.reversalVerifiedAt = reversalVerifiedAt;
            this.reversalVerifiedByAdmin = reversalVerifiedByAdmin;
        }

        public UUID getSettlementId() {
            return settlementId;
        }

        public LocalDate getBusinessDate() {
            return businessDate;
        }

        public SettlementStatus getStatus() {
            return status;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public String getPaymentReference() {
            return paymentReference;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public LocalDateTime getSettledAt() {
            return settledAt;
        }

        public String getReversalReference() {
            return reversalReference;
        }

        public String getReversalReason() {
            return reversalReason;
        }

        public LocalDateTime getReversalVerifiedAt() {
            return reversalVerifiedAt;
        }

        public String getReversalVerifiedByAdmin() {
            return reversalVerifiedByAdmin;
        }
    }
}
