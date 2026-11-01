
package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByBookingBookingIdAndPaymentMethodAndStatus(
            UUID bookingId,
            PaymentMethod paymentMethod,
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
}
