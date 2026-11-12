package com.servicehub.serv.service;

import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Category;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.entity.Customer;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.exception.BookingConflictException;
import com.servicehub.serv.repository.BillingRepository;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.PaymentRepository;
import com.servicehub.serv.repository.WorkerRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBookingServiceTest {

    @Test
    void cancelsOnlyPendingAcceptedAndInProgressBookings() {
        List.of(
                BookingStatus.PENDING,
                BookingStatus.ACCEPTED,
                BookingStatus.IN_PROGRESS)
                .forEach(status -> assertCancellation(status));

        List.of(BookingStatus.values())
                .stream()
                .filter(status -> status != BookingStatus.PENDING
                        && status != BookingStatus.ACCEPTED
                        && status != BookingStatus.IN_PROGRESS)
                .forEach(this::assertCancellationRejected);
    }

    @Test
    void trimsAndPersistsAdminReasonWithoutChangingWorkerCancellationFields() {
        Booking booking = createBooking(BookingStatus.IN_PROGRESS);
        Worker worker = createWorker();
        booking.setWorker(worker);
        booking.setWorkerCancellationReason("Existing worker reason");
        booking.setWorkerCancellationMessage("Existing worker message");

        BookingRepository bookingRepository = mock(BookingRepository.class);
        BillingRepository billingRepository = mock(BillingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);

        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(workerRepository.findByUserIdForUpdate(worker.getUserId()))
                .thenReturn(Optional.of(worker));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(billingRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByBookingBookingIdInOrderByCreatedAtDesc(
                anyCollection()))
                .thenReturn(List.of());

        AdminBookingService service = new AdminBookingService(
                bookingRepository,
                billingRepository,
                paymentRepository,
                workerRepository);

        service.cancelBooking(booking.getBookingId(), "  Admin cancellation  ");

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals("Admin cancellation", booking.getAdminCancellationReason());
        assertEquals("Existing worker reason", booking.getWorkerCancellationReason());
        assertEquals("Existing worker message", booking.getWorkerCancellationMessage());
        assertEquals(AvailabilityStatus.AVAILABLE, worker.getAvailabilityStatus());
        verify(paymentRepository, never()).save(any());
        verify(billingRepository, never()).save(any());
    }

    @Test
    void adminCancellationPreservesOfflineWorkerAvailability() {
        Booking booking = createBooking(BookingStatus.ACCEPTED);
        Worker worker = createWorker();
        worker.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        booking.setWorker(worker);

        BookingRepository bookingRepository = mock(BookingRepository.class);
        BillingRepository billingRepository = mock(BillingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);

        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(workerRepository.findByUserIdForUpdate(worker.getUserId()))
                .thenReturn(Optional.of(worker));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(billingRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByBookingBookingIdInOrderByCreatedAtDesc(
                anyCollection()))
                .thenReturn(List.of());

        AdminBookingService service = new AdminBookingService(
                bookingRepository,
                billingRepository,
                paymentRepository,
                workerRepository);

        service.cancelBooking(booking.getBookingId(), "Reason");

        assertEquals(AvailabilityStatus.UNAVAILABLE, worker.getAvailabilityStatus());
    }

    @Test
    void rejectsBlankCancellationReason() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        AdminBookingService service = new AdminBookingService(
                bookingRepository,
                mock(BillingRepository.class),
                mock(PaymentRepository.class),
                mock(WorkerRepository.class));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.cancelBooking(UUID.randomUUID(), "  "));

        verify(bookingRepository, never()).findByBookingIdForUpdate(any());
    }

    private void assertCancellation(BookingStatus status) {
        Booking booking = createBooking(status);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BillingRepository billingRepository = mock(BillingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);

        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(billingRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByBookingBookingIdInOrderByCreatedAtDesc(
                anyCollection()))
                .thenReturn(List.of());

        AdminBookingService service = new AdminBookingService(
                bookingRepository,
                billingRepository,
                paymentRepository,
                workerRepository);

        service.cancelBooking(booking.getBookingId(), "Reason");

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals("Reason", booking.getAdminCancellationReason());
        verify(paymentRepository, never()).save(any());
        verify(billingRepository, never()).save(any());
    }

    private void assertCancellationRejected(BookingStatus status) {
        Booking booking = createBooking(status);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        AdminBookingService service = new AdminBookingService(
                bookingRepository,
                mock(BillingRepository.class),
                mock(PaymentRepository.class),
                mock(WorkerRepository.class));

        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));

        assertThrows(
                BookingConflictException.class,
                () -> service.cancelBooking(booking.getBookingId(), "Reason"));

        verify(bookingRepository, never()).save(any());
    }

    private Booking createBooking(BookingStatus status) {
        Credentials credentials = new Credentials();
        credentials.setEmail("customer@example.com");

        Users customerUser = new Users();
        customerUser.setUserId(UUID.randomUUID());
        customerUser.setCredentials(credentials);
        customerUser.setFullName("Test Customer");
        customerUser.setPhone("5550100");

        Customer customer = new Customer();
        customer.setUser(customerUser);

        Category category = new Category();
        category.setCategoryName("Test Category");

        Service service = new Service();
        service.setServiceId(UUID.randomUUID());
        service.setServiceName("Test Service");
        service.setCategory(category);

        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setCustomer(customer);
        booking.setService(service);
        booking.setStatus(status);
        booking.setServiceLatitude(BigDecimal.ZERO);
        booking.setServiceLongitude(BigDecimal.ZERO);
        return booking;
    }

    private Worker createWorker() {
        Credentials credentials = new Credentials();
        credentials.setEmail("worker@example.com");

        Users workerUser = new Users();
        workerUser.setUserId(UUID.randomUUID());
        workerUser.setCredentials(credentials);
        workerUser.setFullName("Test Worker");
        workerUser.setPhone("5550200");

        Worker worker = new Worker();
        worker.setUserId(workerUser.getUserId());
        worker.setUser(workerUser);
        worker.setAvailabilityStatus(AvailabilityStatus.BUSY);
        return worker;
    }
}
