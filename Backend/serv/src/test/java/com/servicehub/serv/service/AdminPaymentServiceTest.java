package com.servicehub.serv.service;

import com.servicehub.serv.dto.AdminPaymentDashboardDto;
import com.servicehub.serv.dto.AdminPaymentDetailsDto;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;
import com.servicehub.serv.exception.ResourceNotFoundException;
import com.servicehub.serv.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminPaymentServiceTest {

    @Test
    void summarizesOnlySuccessfulPaymentsAndKeepsEachAttempt() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setBookingId(bookingId);

        List<Payment> payments = List.of(
                createPayment(booking, PaymentStatus.SUCCESS, "120.25"),
                createPayment(booking, PaymentStatus.SUCCESS, "79.75"),
                createPayment(booking, PaymentStatus.PENDING, "300.00"),
                createPayment(booking, PaymentStatus.FAILED, "400.00"),
                createPayment(booking, PaymentStatus.CANCELLED, "500.00"),
                createPayment(booking, PaymentStatus.REFUNDED, "600.00"),
                createPayment(booking, PaymentStatus.CREATED, "700.00"));

        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        when(paymentRepository.findAllForAdmin()).thenReturn(payments);

        AdminPaymentDashboardDto dashboard =
                new AdminPaymentService(paymentRepository).getPayments();

        assertEquals(2, dashboard.summary().successfulCount());
        assertEquals(1, dashboard.summary().pendingCount());
        assertEquals(1, dashboard.summary().failedCount());
        assertEquals(1, dashboard.summary().cancelledCount());
        assertEquals(1, dashboard.summary().refundedCount());
        assertEquals(
                new BigDecimal("200.00"),
                dashboard.summary().successfullyCollectedAmount());
        assertEquals(7, dashboard.payments().size());
        assertEquals(
                bookingId,
                dashboard.payments().get(0).bookingId());
        assertEquals(
                bookingId,
                dashboard.payments().get(1).bookingId());
    }

    @Test
    void returnsAllAttemptsForTheSelectedPaymentsBookingWithoutMutatingThem() {
        UUID bookingId = UUID.randomUUID();
        Payment selectedPayment = createPayment(
                bookingId,
                PaymentStatus.CANCELLED,
                "50.00");
        Payment successfulPayment = createPayment(
                bookingId,
                PaymentStatus.SUCCESS,
                "50.00");
        List<Payment> relatedPayments =
                List.of(successfulPayment, selectedPayment);

        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        when(paymentRepository.findByPaymentId(selectedPayment.getPaymentId()))
                .thenReturn(Optional.of(selectedPayment));
        when(paymentRepository.findByBookingBookingIdOrderByCreatedAtDesc(
                bookingId))
                .thenReturn(relatedPayments);

        AdminPaymentDetailsDto details =
                new AdminPaymentService(paymentRepository)
                        .getPayment(selectedPayment.getPaymentId());

        assertEquals(selectedPayment.getPaymentId(), details.paymentId());
        assertEquals(2, details.relatedAttempts().size());
        assertEquals(
                PaymentStatus.SUCCESS,
                details.relatedAttempts().get(0).status());
        assertEquals(
                PaymentStatus.CANCELLED,
                details.relatedAttempts().get(1).status());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void reportsMissingPayment() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        when(paymentRepository.findByPaymentId(any(UUID.class)))
                .thenReturn(Optional.empty());

        AdminPaymentService service = new AdminPaymentService(paymentRepository);

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getPayment(UUID.randomUUID()));
    }

    private Payment createPayment(
            Booking booking,
            PaymentStatus status,
            String amount) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        payment.setBooking(booking);
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal(amount));
        return payment;
    }

    private Payment createPayment(
            UUID bookingId,
            PaymentStatus status,
            String amount) {
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        return createPayment(booking, status, amount);
    }
}
