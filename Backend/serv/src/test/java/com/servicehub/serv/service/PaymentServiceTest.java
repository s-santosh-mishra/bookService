package com.servicehub.serv.service;

import com.razorpay.RazorpayClient;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;
import com.servicehub.serv.repository.BillingRepository;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.PaymentRepository;
import com.servicehub.serv.repository.WorkerRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    @Test
    void cancelsOtherPendingPaymentsAfterCashPaymentSucceeds() {
        UUID bookingId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        Users workerUser = new Users();
        workerUser.setUserId(workerId);

        Worker worker = new Worker();
        worker.setUserId(workerId);
        worker.setUser(workerUser);
        worker.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);

        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setWorker(worker);
        booking.setStatus(BookingStatus.WAITING_FOR_PAYMENT);

        Payment cashPayment = createPayment(
                booking,
                PaymentMethod.CASH,
                PaymentStatus.PENDING);
        Payment pendingRazorpayPayment = createPayment(
                booking,
                PaymentMethod.RAZORPAY,
                PaymentStatus.PENDING);

        BookingRepository bookingRepository = mock(BookingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);

        when(bookingRepository.findByBookingIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingBookingIdAndPaymentMethodAndStatus(
                bookingId,
                PaymentMethod.CASH,
                PaymentStatus.PENDING))
                .thenReturn(Optional.of(cashPayment));
        when(paymentRepository.findByBookingBookingIdAndStatus(
                bookingId,
                PaymentStatus.PENDING))
                .thenReturn(List.of(pendingRazorpayPayment));
        when(workerRepository.findByUserIdForUpdate(workerId))
                .thenReturn(Optional.of(worker));

        PaymentService paymentService = new PaymentService(
                mock(RazorpayClient.class),
                paymentRepository,
                bookingRepository,
                mock(BillingRepository.class),
                workerRepository,
                Clock.systemUTC());

        paymentService.confirmCashPayment(workerId, bookingId);

        assertEquals(PaymentStatus.SUCCESS, cashPayment.getStatus());
        assertEquals(PaymentStatus.CANCELLED, pendingRazorpayPayment.getStatus());
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertEquals(AvailabilityStatus.UNAVAILABLE, worker.getAvailabilityStatus());
        verify(paymentRepository).saveAll(List.of(pendingRazorpayPayment));
    }

    private Payment createPayment(
            Booking booking,
            PaymentMethod method,
            PaymentStatus status) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(method);
        payment.setStatus(status);
        return payment;
    }
}
