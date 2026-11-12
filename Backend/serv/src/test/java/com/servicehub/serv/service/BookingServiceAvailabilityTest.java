package com.servicehub.serv.service;

import com.servicehub.serv.dto.WorkerBookingLocationDto;
import com.servicehub.serv.dto.WorkerBookingRequestDto;
import com.servicehub.serv.dto.WorkerCancellationRequestDto;
import com.servicehub.serv.dto.CreateBookingDto;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.BookingOffer;
import com.servicehub.serv.entity.BookingPart;
import com.servicehub.serv.entity.Customer;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.entity.WorkerService;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.PartApprovalStatus;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.repository.BillingRepository;
import com.servicehub.serv.repository.BookingOfferRepository;
import com.servicehub.serv.repository.BookingPartRepository;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.CustomerRepository;
import com.servicehub.serv.repository.PaymentRepository;
import com.servicehub.serv.repository.ServiceRepository;
import com.servicehub.serv.repository.WorkerBookingRejectionRepository;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.repository.WorkerServiceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceAvailabilityTest {

    @Test
    void createBookingRejectsCustomerLocationOutsideOdisha() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        OdishaServiceArea odishaServiceArea = mock(OdishaServiceArea.class);
        when(odishaServiceArea.contains(0.0, 0.0)).thenReturn(false);
        BookingService service = createBookingService(
                bookingRepository,
                mock(WorkerRepository.class),
                mock(WorkerServiceRepository.class),
                mock(BillingRepository.class),
                mock(PaymentRepository.class),
                mock(CustomerRepository.class),
                mock(BillingService.class),
                mock(BookingOfferRepository.class),
                odishaServiceArea);
        CreateBookingDto request = new CreateBookingDto();
        request.setServiceId(UUID.randomUUID());
        request.setLatitude(BigDecimal.ZERO);
        request.setLongitude(BigDecimal.ZERO);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createBooking(UUID.randomUUID(), request));
        verify(bookingRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unavailableWorkerDoesNotReceiveBookingRequests() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BookingOfferRepository bookingOfferRepository = mock(BookingOfferRepository.class);
        Worker worker = createWorker(AvailabilityStatus.UNAVAILABLE);
        when(workerRepository.findById(worker.getUserId()))
                .thenReturn(Optional.of(worker));

        BookingService service = createBookingService(
                bookingRepository,
                workerRepository,
                mock(WorkerServiceRepository.class),
                bookingOfferRepository);

        assertTrue(service.getWorkerBookingRequests(worker.getUserId()).isEmpty());
        verify(bookingOfferRepository, never())
                .findPendingOffersForWorker(worker.getUserId(), BookingStatus.PENDING);
    }

    @Test
    void busyWorkerDoesNotReceiveBookingRequests() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BookingOfferRepository bookingOfferRepository = mock(BookingOfferRepository.class);
        Worker worker = createWorker(AvailabilityStatus.BUSY);
        when(workerRepository.findById(worker.getUserId()))
                .thenReturn(Optional.of(worker));

        BookingService service = createBookingService(
                bookingRepository,
                workerRepository,
                mock(WorkerServiceRepository.class),
                bookingOfferRepository);

        assertTrue(service.getWorkerBookingRequests(worker.getUserId()).isEmpty());
        verify(bookingOfferRepository, never())
                .findPendingOffersForWorker(worker.getUserId(), BookingStatus.PENDING);
    }

    @Test
    void workerWithAnActiveBookingDoesNotReceiveRequestsEvenIfStatusIsAvailable() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BookingOfferRepository bookingOfferRepository = mock(BookingOfferRepository.class);
        Worker worker = createWorker(AvailabilityStatus.AVAILABLE);
        when(workerRepository.findById(worker.getUserId()))
                .thenReturn(Optional.of(worker));
        when(bookingRepository.existsByWorkerUserIdAndStatusIn(
                worker.getUserId(),
                List.of(
                        BookingStatus.ACCEPTED,
                        BookingStatus.IN_PROGRESS,
                        BookingStatus.WAITING_FOR_PAYMENT)))
                .thenReturn(true);

        BookingService service = createBookingService(
                bookingRepository,
                workerRepository,
                mock(WorkerServiceRepository.class),
                bookingOfferRepository);

        assertTrue(service.getWorkerBookingRequests(worker.getUserId()).isEmpty());
        verify(bookingOfferRepository, never())
                .findPendingOffersForWorker(worker.getUserId(), BookingStatus.PENDING);
    }

    @Test
    void availableWorkerReceivesEligibleBookingRequests() {
        Worker worker = createWorker(AvailabilityStatus.AVAILABLE);
        Service serviceEntity = createService();
        Customer customer = createCustomer();
        Booking booking = createBooking(
                BookingStatus.PENDING,
                customer,
                null,
                serviceEntity);

        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        WorkerServiceRepository workerServiceRepository =
                mock(WorkerServiceRepository.class);
        BookingOfferRepository bookingOfferRepository =
                mock(BookingOfferRepository.class);

        when(workerRepository.findById(worker.getUserId()))
                .thenReturn(Optional.of(worker));
        when(bookingOfferRepository.findPendingOffersForWorker(
                worker.getUserId(), BookingStatus.PENDING))
                .thenReturn(List.of(new BookingOffer(booking, worker)));

        BookingService bookingService = createBookingService(
                bookingRepository,
                workerRepository,
                workerServiceRepository,
                bookingOfferRepository);

        List<WorkerBookingRequestDto> requests =
                bookingService.getWorkerBookingRequests(worker.getUserId());

        assertEquals(1, requests.size());
        assertEquals(booking.getBookingId(), requests.get(0).getBookingId());
    }

    @Test
    void workerDoesNotReceiveExpiredOffers() {
        Worker worker = createWorker(AvailabilityStatus.AVAILABLE);
        Booking booking = createBooking(
                BookingStatus.PENDING,
                createCustomer(),
                null,
                createService());
        setCreatedAt(booking, LocalDateTime.now().minusMinutes(30));
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BookingOfferRepository bookingOfferRepository = mock(BookingOfferRepository.class);
        when(workerRepository.findById(worker.getUserId()))
                .thenReturn(Optional.of(worker));
        when(bookingOfferRepository.findPendingOffersForWorker(
                worker.getUserId(), BookingStatus.PENDING))
                .thenReturn(List.of(new BookingOffer(booking, worker)));

        BookingService bookingService = createBookingService(
                bookingRepository,
                workerRepository,
                mock(WorkerServiceRepository.class),
                bookingOfferRepository);

        assertTrue(bookingService.getWorkerBookingRequests(worker.getUserId()).isEmpty());
    }

    @Test
    void workerCannotAcceptBookingThatWasNotOfferedToThem() {
        Worker worker = createWorker(AvailabilityStatus.AVAILABLE);
        Booking booking = createBooking(
                BookingStatus.PENDING,
                createCustomer(),
                null,
                createService());
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BookingOfferRepository bookingOfferRepository = mock(BookingOfferRepository.class);
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(bookingOfferRepository.existsByBooking_BookingIdAndWorker_UserId(
                booking.getBookingId(), worker.getUserId()))
                .thenReturn(false);

        BookingService bookingService = createBookingService(
                bookingRepository,
                workerRepository,
                mock(WorkerServiceRepository.class),
                mock(BillingRepository.class),
                mock(PaymentRepository.class),
                bookingOfferRepository);

        assertThrows(IllegalStateException.class, () -> bookingService.acceptBooking(
                worker.getUserId(),
                booking.getBookingId(),
                new WorkerBookingLocationDto()));
        verify(workerRepository, never()).findByUserIdForUpdate(worker.getUserId());
    }

    @Test
    void workerCannotAcceptExpiredOffer() {
        Worker worker = createWorker(AvailabilityStatus.AVAILABLE);
        Booking booking = createBooking(
                BookingStatus.PENDING,
                createCustomer(),
                null,
                createService());
        setCreatedAt(booking, LocalDateTime.now().minusMinutes(30));
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BookingOfferRepository bookingOfferRepository = mock(BookingOfferRepository.class);
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(bookingOfferRepository.existsByBooking_BookingIdAndWorker_UserId(
                booking.getBookingId(), worker.getUserId()))
                .thenReturn(true);
        BookingService bookingService = createBookingService(
                bookingRepository,
                workerRepository,
                mock(WorkerServiceRepository.class),
                mock(BillingRepository.class),
                mock(PaymentRepository.class),
                bookingOfferRepository);

        assertThrows(IllegalStateException.class, () -> bookingService.acceptBooking(
                worker.getUserId(),
                booking.getBookingId(),
                new WorkerBookingLocationDto()));
        verify(workerRepository, never()).findByUserIdForUpdate(worker.getUserId());
    }

    @Test
    void workerCannotAcceptAnOfferTheyAlreadyRejected() {
        Worker worker = createWorker(AvailabilityStatus.AVAILABLE);
        Booking booking = createBooking(
                BookingStatus.PENDING,
                createCustomer(),
                null,
                createService());
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BookingOfferRepository bookingOfferRepository = mock(BookingOfferRepository.class);
        WorkerBookingRejectionRepository rejectionRepository =
                mock(WorkerBookingRejectionRepository.class);
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(bookingOfferRepository.existsByBooking_BookingIdAndWorker_UserId(
                booking.getBookingId(), worker.getUserId()))
                .thenReturn(true);
        when(rejectionRepository.existsByWorker_UserIdAndBooking_BookingId(
                worker.getUserId(), booking.getBookingId()))
                .thenReturn(true);

        BookingService bookingService = createLifecycleService(
                bookingRepository,
                workerRepository,
                mock(BookingPartRepository.class),
                mock(BillingService.class),
                mock(BillingRepository.class),
                mock(PaymentRepository.class),
                rejectionRepository);

        assertThrows(IllegalStateException.class, () -> bookingService.acceptBooking(
                worker.getUserId(),
                booking.getBookingId(),
                new WorkerBookingLocationDto()));
        verify(workerRepository, never()).findByUserIdForUpdate(worker.getUserId());
    }

    @Test
    void workerWithStaleLocationCannotAcceptAnOffer() {
        Worker worker = createWorker(AvailabilityStatus.AVAILABLE);
        worker.setLocationLastUpdatedAt(LocalDateTime.now(Clock.systemUTC()).minusMinutes(4));
        Booking booking = createBooking(
                BookingStatus.PENDING,
                createCustomer(),
                null,
                createService());
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        WorkerServiceRepository workerServiceRepository = mock(WorkerServiceRepository.class);
        BookingOfferRepository bookingOfferRepository = mock(BookingOfferRepository.class);
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(bookingOfferRepository.existsByBooking_BookingIdAndWorker_UserId(
                booking.getBookingId(), worker.getUserId()))
                .thenReturn(true);
        when(workerRepository.findByUserIdForUpdate(worker.getUserId()))
                .thenReturn(Optional.of(worker));

        BookingService bookingService = createBookingService(
                bookingRepository,
                workerRepository,
                workerServiceRepository,
                mock(BillingRepository.class),
                mock(PaymentRepository.class),
                bookingOfferRepository);

        assertThrows(IllegalStateException.class, () -> bookingService.acceptBooking(
                worker.getUserId(),
                booking.getBookingId(),
                new WorkerBookingLocationDto()));
        verify(workerServiceRepository, never()).findByWorker_UserId(worker.getUserId());
    }

    @Test
    void startOtpIsNotReplacedByRepeatedRequestsAndIsConsumedOnStart() {
        Worker worker = createWorker(AvailabilityStatus.BUSY);
        Booking booking = createBooking(
                BookingStatus.ACCEPTED,
                createCustomer(),
                worker,
                createService());
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BillingRepository billingRepository = mock(BillingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        when(workerRepository.findById(worker.getUserId())).thenReturn(Optional.of(worker));
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(billingRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBookingBookingIdOrderByCreatedAtDesc(
                booking.getBookingId())).thenReturn(Optional.empty());
        BookingService service = createLifecycleService(
                bookingRepository,
                workerRepository,
                mock(BookingPartRepository.class),
                mock(BillingService.class),
                billingRepository,
                paymentRepository);

        service.requestStart(worker.getUserId(), booking.getBookingId());
        String startOtp = booking.getStartOtp();
        assertTrue(startOtp.matches("\\d{4}"));
        assertThrows(IllegalStateException.class,
                () -> service.requestStart(worker.getUserId(), booking.getBookingId()));
        assertEquals(startOtp, booking.getStartOtp());

        assertThrows(IllegalArgumentException.class,
                () -> service.startBooking(worker.getUserId(), booking.getBookingId(), "12x4"));
        assertEquals(BookingStatus.ACCEPTED, booking.getStatus());

        service.startBooking(worker.getUserId(), booking.getBookingId(), startOtp);
        assertEquals(BookingStatus.IN_PROGRESS, booking.getStatus());
        assertEquals(null, booking.getStartOtp());
        assertThrows(IllegalStateException.class,
                () -> service.startBooking(worker.getUserId(), booking.getBookingId(), startOtp));
    }

    @Test
    void completionRequiresCorrectOtpAndCannotProcessTwice() {
        Worker worker = createWorker(AvailabilityStatus.BUSY);
        Booking booking = createBooking(
                BookingStatus.IN_PROGRESS,
                createCustomer(),
                worker,
                createService());
        booking.setCompletionOtp("0123");
        booking.setCompletionRequested(true);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BillingService billingService = mock(BillingService.class);
        BillingRepository billingRepository = mock(BillingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        when(workerRepository.findById(worker.getUserId())).thenReturn(Optional.of(worker));
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(billingRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBookingBookingIdOrderByCreatedAtDesc(
                booking.getBookingId())).thenReturn(Optional.empty());
        BookingService service = createLifecycleService(
                bookingRepository,
                workerRepository,
                mock(BookingPartRepository.class),
                billingService,
                billingRepository,
                paymentRepository);

        assertThrows(IllegalArgumentException.class,
                () -> service.completeBooking(worker.getUserId(), booking.getBookingId(), "9999"));
        assertEquals(BookingStatus.IN_PROGRESS, booking.getStatus());
        assertEquals("0123", booking.getCompletionOtp());

        service.completeBooking(worker.getUserId(), booking.getBookingId(), "0123");
        assertEquals(BookingStatus.WAITING_FOR_PAYMENT, booking.getStatus());
        assertEquals(null, booking.getCompletionOtp());
        assertThrows(IllegalStateException.class,
                () -> service.completeBooking(worker.getUserId(), booking.getBookingId(), "0123"));
        verify(billingService, times(1)).createBilling(booking);
    }

    @Test
    void completionRequestCannotBeRepeated() {
        Worker worker = createWorker(AvailabilityStatus.BUSY);
        Booking booking = createBooking(
                BookingStatus.IN_PROGRESS,
                createCustomer(),
                worker,
                createService());
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BookingPartRepository bookingPartRepository = mock(BookingPartRepository.class);
        BillingRepository billingRepository = mock(BillingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        when(workerRepository.findById(worker.getUserId())).thenReturn(Optional.of(worker));
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingPartRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(List.of());
        when(billingRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBookingBookingIdOrderByCreatedAtDesc(
                booking.getBookingId())).thenReturn(Optional.empty());
        BookingService service = createLifecycleService(
                bookingRepository,
                workerRepository,
                bookingPartRepository,
                mock(BillingService.class),
                billingRepository,
                paymentRepository);

        service.requestCompletion(worker.getUserId(), booking.getBookingId());
        String completionOtp = booking.getCompletionOtp();
        assertTrue(completionOtp.matches("\\d{4}"));
        assertThrows(IllegalStateException.class,
                () -> service.requestCompletion(worker.getUserId(), booking.getBookingId()));
        assertEquals(completionOtp, booking.getCompletionOtp());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void completionCannotBeRequestedWhilePartsArePending() {
        Worker worker = createWorker(AvailabilityStatus.BUSY);
        Booking booking = createBooking(
                BookingStatus.IN_PROGRESS,
                createCustomer(),
                worker,
                createService());
        BookingPart pendingPart = new BookingPart();
        pendingPart.setStatus(PartApprovalStatus.PENDING);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BookingPartRepository bookingPartRepository = mock(BookingPartRepository.class);
        when(workerRepository.findById(worker.getUserId())).thenReturn(Optional.of(worker));
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(bookingPartRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(List.of(pendingPart));
        BookingService service = createLifecycleService(
                bookingRepository,
                workerRepository,
                bookingPartRepository,
                mock(BillingService.class),
                mock(BillingRepository.class),
                mock(PaymentRepository.class));

        assertThrows(IllegalStateException.class,
                () -> service.requestCompletion(worker.getUserId(), booking.getBookingId()));
        assertEquals(false, booking.isCompletionRequested());
        verify(bookingRepository, never()).save(booking);
    }

    @Test
    void workerCancellationMakesBusyWorkerAvailable() {
        assertWorkerCancellationAvailability(
                AvailabilityStatus.BUSY,
                AvailabilityStatus.AVAILABLE);
    }

    @Test
    void workerCancellationPreservesOfflineAvailability() {
        assertWorkerCancellationAvailability(
                AvailabilityStatus.UNAVAILABLE,
                AvailabilityStatus.UNAVAILABLE);
    }

    @Test
    void acceptingBookingMarksWorkerBusy() {
        Worker worker = createWorker(AvailabilityStatus.AVAILABLE);
        Service serviceEntity = createService();
        Customer customer = createCustomer();
        Booking booking = createBooking(
                BookingStatus.PENDING,
                customer,
                null,
                serviceEntity);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        WorkerServiceRepository workerServiceRepository =
                mock(WorkerServiceRepository.class);
        BookingOfferRepository bookingOfferRepository =
                mock(BookingOfferRepository.class);
        BillingRepository billingRepository = mock(BillingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);

        when(workerRepository.findByUserIdForUpdate(worker.getUserId()))
                .thenReturn(Optional.of(worker));
        when(workerServiceRepository.findByWorker_UserId(worker.getUserId()))
                .thenReturn(List.of(new WorkerService(worker, serviceEntity)));
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(bookingOfferRepository.existsByBooking_BookingIdAndWorker_UserId(
                booking.getBookingId(), worker.getUserId()))
                .thenReturn(true);
        when(bookingRepository.existsByWorkerUserIdAndStatusIn(
                worker.getUserId(),
                List.of(
                        BookingStatus.ACCEPTED,
                        BookingStatus.IN_PROGRESS,
                        BookingStatus.WAITING_FOR_PAYMENT)))
                .thenReturn(false);
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(billingRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBookingBookingIdOrderByCreatedAtDesc(
                booking.getBookingId()))
                .thenReturn(Optional.empty());

        BookingService bookingService = createBookingService(
                bookingRepository,
                workerRepository,
                workerServiceRepository,
                billingRepository,
                paymentRepository,
                bookingOfferRepository);
        WorkerBookingLocationDto location = new WorkerBookingLocationDto();
        location.setLatitude(BigDecimal.ZERO);
        location.setLongitude(BigDecimal.ZERO);

        bookingService.acceptBooking(
                worker.getUserId(),
                booking.getBookingId(),
                location);

        assertEquals(BookingStatus.ACCEPTED, booking.getStatus());
        assertEquals(AvailabilityStatus.BUSY, worker.getAvailabilityStatus());

        UUID secondWorkerId = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> bookingService.acceptBooking(
                secondWorkerId,
                booking.getBookingId(),
                location));
        assertEquals(worker.getUserId(), booking.getWorker().getUserId());
        verify(workerRepository, never()).findByUserIdForUpdate(secondWorkerId);
    }

    @Test
    void customerCancellationPreservesOfflineWorkerAvailability() {
        UUID customerId = UUID.randomUUID();
        Worker worker = createWorker(AvailabilityStatus.UNAVAILABLE);
        Customer customer = createCustomer();
        customer.setUserId(customerId);
        customer.getUser().setUserId(customerId);
        Booking booking = createBooking(
                BookingStatus.ACCEPTED,
                customer,
                worker,
                createService());
        BookingRepository bookingRepository = mock(BookingRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BillingRepository billingRepository = mock(BillingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);

        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));
        when(workerRepository.findByUserIdForUpdate(worker.getUserId()))
                .thenReturn(Optional.of(worker));
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(billingRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBookingBookingIdOrderByCreatedAtDesc(
                booking.getBookingId()))
                .thenReturn(Optional.empty());

        BookingService bookingService = createBookingService(
                bookingRepository,
                workerRepository,
                mock(WorkerServiceRepository.class),
                billingRepository,
                paymentRepository,
                customerRepository,
                mock(BillingService.class));

        bookingService.cancelBooking(customerId, booking.getBookingId());

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(AvailabilityStatus.UNAVAILABLE, worker.getAvailabilityStatus());
    }

    private BookingService createBookingService(
            BookingRepository bookingRepository,
            WorkerRepository workerRepository,
            WorkerServiceRepository workerServiceRepository) {
        return createBookingService(
                bookingRepository,
                workerRepository,
                workerServiceRepository,
                mock(BillingRepository.class),
                mock(PaymentRepository.class));
    }

    private BookingService createBookingService(
            BookingRepository bookingRepository,
            WorkerRepository workerRepository,
            WorkerServiceRepository workerServiceRepository,
            BookingOfferRepository bookingOfferRepository) {
        return createBookingService(
                bookingRepository,
                workerRepository,
                workerServiceRepository,
                mock(BillingRepository.class),
                mock(PaymentRepository.class),
                bookingOfferRepository);
    }

    private BookingService createBookingService(
            BookingRepository bookingRepository,
            WorkerRepository workerRepository,
            WorkerServiceRepository workerServiceRepository,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository) {
        return createBookingService(
                bookingRepository,
                workerRepository,
                workerServiceRepository,
                billingRepository,
                paymentRepository,
                mock(BookingOfferRepository.class));
    }

    private BookingService createBookingService(
            BookingRepository bookingRepository,
            WorkerRepository workerRepository,
            WorkerServiceRepository workerServiceRepository,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            BookingOfferRepository bookingOfferRepository) {
        return createBookingService(
                bookingRepository,
                workerRepository,
                workerServiceRepository,
                billingRepository,
                paymentRepository,
                mock(CustomerRepository.class),
                mock(BillingService.class),
                bookingOfferRepository);
    }

    private BookingService createBookingService(
            BookingRepository bookingRepository,
            WorkerRepository workerRepository,
            WorkerServiceRepository workerServiceRepository,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            CustomerRepository customerRepository,
            BillingService billingService) {
        return createBookingService(
                bookingRepository,
                workerRepository,
                workerServiceRepository,
                billingRepository,
                paymentRepository,
                customerRepository,
                billingService,
                mock(BookingOfferRepository.class));
    }

    private BookingService createBookingService(
            BookingRepository bookingRepository,
            WorkerRepository workerRepository,
            WorkerServiceRepository workerServiceRepository,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            CustomerRepository customerRepository,
            BillingService billingService,
            BookingOfferRepository bookingOfferRepository) {
        return createBookingService(
                bookingRepository,
                workerRepository,
                workerServiceRepository,
                billingRepository,
                paymentRepository,
                customerRepository,
                billingService,
                bookingOfferRepository,
                mock(OdishaServiceArea.class));
    }

    private BookingService createBookingService(
            BookingRepository bookingRepository,
            WorkerRepository workerRepository,
            WorkerServiceRepository workerServiceRepository,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            CustomerRepository customerRepository,
            BillingService billingService,
            BookingOfferRepository bookingOfferRepository,
            OdishaServiceArea odishaServiceArea) {
        return new BookingService(
                bookingRepository,
                mock(BookingPartRepository.class),
                customerRepository,
                mock(ServiceRepository.class),
                workerRepository,
                workerServiceRepository,
                mock(WorkerBookingRejectionRepository.class),
                bookingOfferRepository,
                mock(BookingDiscoveryService.class),
                Clock.systemUTC(),
                odishaServiceArea,
                billingService,
                billingRepository,
                paymentRepository);
    }

    private BookingService createLifecycleService(
            BookingRepository bookingRepository,
            WorkerRepository workerRepository,
            BookingPartRepository bookingPartRepository,
            BillingService billingService,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository) {
        return createLifecycleService(
                bookingRepository,
                workerRepository,
                bookingPartRepository,
                billingService,
                billingRepository,
                paymentRepository,
                mock(WorkerBookingRejectionRepository.class));
    }

    private BookingService createLifecycleService(
            BookingRepository bookingRepository,
            WorkerRepository workerRepository,
            BookingPartRepository bookingPartRepository,
            BillingService billingService,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            WorkerBookingRejectionRepository rejectionRepository) {
        return new BookingService(
                bookingRepository,
                bookingPartRepository,
                mock(CustomerRepository.class),
                mock(ServiceRepository.class),
                workerRepository,
                mock(WorkerServiceRepository.class),
                rejectionRepository,
                mock(BookingOfferRepository.class),
                mock(BookingDiscoveryService.class),
                Clock.systemUTC(),
                mock(OdishaServiceArea.class),
                billingService,
                billingRepository,
                paymentRepository);
    }

    private void assertWorkerCancellationAvailability(
            AvailabilityStatus initialStatus,
            AvailabilityStatus expectedStatus) {
        Worker worker = createWorker(initialStatus);
        Booking booking = createBooking(
                BookingStatus.ACCEPTED,
                createCustomer(),
                worker,
                createService());
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        BillingRepository billingRepository = mock(BillingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(workerRepository.findByUserIdForUpdate(worker.getUserId()))
                .thenReturn(Optional.of(worker));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(billingRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBookingBookingIdOrderByCreatedAtDesc(
                booking.getBookingId())).thenReturn(Optional.empty());
        BookingService service = createLifecycleService(
                bookingRepository,
                workerRepository,
                mock(BookingPartRepository.class),
                mock(BillingService.class),
                billingRepository,
                paymentRepository);
        WorkerCancellationRequestDto request = new WorkerCancellationRequestDto();
        request.setReason("EMERGENCY_PERSONAL_ISSUE");

        service.workerCancelBooking(worker.getUserId(), booking.getBookingId(), request);

        assertEquals(BookingStatus.WORKER_CANCELLED, booking.getStatus());
        assertEquals(expectedStatus, worker.getAvailabilityStatus());
        assertThrows(IllegalStateException.class,
                () -> service.workerCancelBooking(worker.getUserId(), booking.getBookingId(), request));
    }

    private Worker createWorker(AvailabilityStatus availabilityStatus) {
        UUID workerId = UUID.randomUUID();
        Users user = new Users();
        user.setUserId(workerId);
        user.setFullName("Test Worker");
        user.setActive(true);

        Worker worker = new Worker();
        worker.setUserId(workerId);
        worker.setUser(user);
        worker.setVerificationStatus(VerificationStatus.VERIFIED);
        worker.setAvailabilityStatus(availabilityStatus);
        worker.setCurrentLatitude(BigDecimal.valueOf(20.0));
        worker.setCurrentLongitude(BigDecimal.valueOf(85.0));
        worker.setLocationLastUpdatedAt(LocalDateTime.now(Clock.systemUTC()));
        return worker;
    }

    private Customer createCustomer() {
        UUID customerId = UUID.randomUUID();
        Users user = new Users();
        user.setUserId(customerId);
        user.setFullName("Test Customer");
        user.setPhone("5550100");
        user.setAddressLine1("1 Main Street");
        user.setCity("Test City");

        Customer customer = new Customer();
        customer.setUserId(customerId);
        customer.setUser(user);
        return customer;
    }

    private Service createService() {
        Service service = new Service();
        service.setServiceId(UUID.randomUUID());
        service.setServiceName("Test Service");
        return service;
    }

    private Booking createBooking(
            BookingStatus status,
            Customer customer,
            Worker worker,
            Service service) {
        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setCustomer(customer);
        booking.setWorker(worker);
        booking.setService(service);
        booking.setStatus(status);
        booking.setServiceLatitude(BigDecimal.ZERO);
        booking.setServiceLongitude(BigDecimal.ZERO);
        setCreatedAt(booking, LocalDateTime.now());
        return booking;
    }

    private void setCreatedAt(Booking booking, LocalDateTime createdAt) {
        try {
            Field field = Booking.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(booking, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
