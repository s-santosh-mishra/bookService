package com.servicehub.serv.service;

import com.servicehub.serv.dto.BookingDto;
import com.servicehub.serv.dto.CreateBookingDto;
import com.servicehub.serv.dto.WorkerBookingLocationDto;
import com.servicehub.serv.dto.WorkerBookingRequestDto;
import com.servicehub.serv.dto.WorkerCancellationRequestDto;
import com.servicehub.serv.entity.*;
import com.servicehub.serv.enums.*;
import com.servicehub.serv.repository.*;

import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingPartRepository bookingPartRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final WorkerRepository workerRepository;
    private final WorkerServiceRepository workerServiceRepository;
    private final WorkerBookingRejectionRepository workerBookingRejectionRepository;
    private final BookingOfferRepository bookingOfferRepository;
    private final BookingDiscoveryService bookingDiscoveryService;
    private final Clock locationClock;
    private final OdishaServiceArea odishaServiceArea;
    private final BillingService billingService;
    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;

    public BookingService(
            BookingRepository bookingRepository,
            BookingPartRepository bookingPartRepository,
            CustomerRepository customerRepository,
            ServiceRepository serviceRepository,
            WorkerRepository workerRepository,
            WorkerServiceRepository workerServiceRepository,
            WorkerBookingRejectionRepository workerBookingRejectionRepository,
            BookingOfferRepository bookingOfferRepository,
            BookingDiscoveryService bookingDiscoveryService,
            Clock locationClock,
            OdishaServiceArea odishaServiceArea,
            BillingService billingService,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository) {

        this.bookingRepository = bookingRepository;
        this.bookingPartRepository = bookingPartRepository;
        this.customerRepository = customerRepository;
        this.serviceRepository = serviceRepository;
        this.workerRepository = workerRepository;
        this.workerServiceRepository = workerServiceRepository;
        this.workerBookingRejectionRepository = workerBookingRejectionRepository;
        this.bookingOfferRepository = bookingOfferRepository;
        this.bookingDiscoveryService = bookingDiscoveryService;
        this.locationClock = locationClock;
        this.odishaServiceArea = odishaServiceArea;
        this.billingService = billingService;
        this.billingRepository = billingRepository;
        this.paymentRepository = paymentRepository;
    }

    private List<BookingStatus> customerActiveStatuses() {
        return List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS);
    }

    @Transactional
    public BookingDto createBooking(UUID customerId, CreateBookingDto request) {
        if (request == null
                || request.getLatitude() == null
                || request.getLongitude() == null
                || !odishaServiceArea.contains(
                        request.getLatitude().doubleValue(),
                        request.getLongitude().doubleValue())) {
            throw new IllegalArgumentException(
                    "Booking location must be within the Odisha service area.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found."));
        Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found."));
        if (!service.isActive())
            throw new IllegalStateException("Cannot book an inactive service.");
        List<BookingStatus> active = customerActiveStatuses();
        if (bookingRepository.existsByCustomerUserIdAndServiceServiceIdAndStatusIn(customerId, request.getServiceId(),
                active))
            throw new IllegalStateException("You already have an active booking for this service.");
        if (bookingRepository.countByCustomerUserIdAndStatusIn(customerId, active) >= 3)
            throw new IllegalStateException("Maximum 3 active bookings are allowed.");
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setService(service);
        booking.setWorker(null);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCustomerNote(request.getCustomerNote());
        booking.setServiceLatitude(request.getLatitude());
        booking.setServiceLongitude(request.getLongitude());

        Booking savedBooking = bookingRepository.save(booking);
        bookingDiscoveryService.discoverBooking(savedBooking.getBookingId());
        return toDto(savedBooking);
    }

    @Transactional
    public BookingDto cancelBooking(UUID customerId, UUID bookingId) {

        customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found."));

        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found."));

        if (!booking.getCustomer().getUserId().equals(customerId)) {
            throw new IllegalStateException(
                    "Booking does not belong to this customer.");
        }

        BookingStatus status = booking.getStatus();

        /*
         * PENDING:
         * Customer can cancel without any charge.
         */
        if (status == BookingStatus.PENDING) {

            booking.setStatus(BookingStatus.CANCELLED);

            booking.setCancelledAt(LocalDateTime.now());

            return toDto(
                    bookingRepository.save(booking));
        }

        /*
         * ACCEPTED or IN_PROGRESS:
         * Customer cancellation is allowed,
         * but billing rules apply.
         */
        if (status != BookingStatus.ACCEPTED
                && status != BookingStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "This booking cannot be cancelled. Please contact support.");
        }

        LocalDateTime cancellationTime = LocalDateTime.now();

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(cancellationTime);

        markWorkerAvailableAfterBooking(booking);

        /*
         * Create the cancellation billing snapshot
         * before returning the booking.
         */
        billingService.createCustomerCancellationBilling(
                booking,
                cancellationTime);

        return toDto(
                bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto requestStart(UUID workerId, UUID bookingId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));

        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (booking.getWorker() == null
                || !booking.getWorker().getUserId().equals(workerId)) {
            throw new IllegalStateException(
                    "Booking is not assigned to this worker.");
        }

        if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException(
                    "Only verified workers can start bookings.");
        }

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "Booking cannot be started.");
        }

        if (booking.getStartOtp() != null) {
            throw new IllegalStateException(
                    "Start OTP has already been requested. Ask the customer for the OTP.");
        }

        booking.setStartOtp(generateOtp());

        return toDto(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto acceptBooking(
            UUID workerId,
            UUID bookingId,
            WorkerBookingLocationDto request) {

        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (booking.getStatus() != BookingStatus.PENDING)
            throw new IllegalStateException("Booking is no longer available.");

        if (booking.getWorker() != null)
            throw new IllegalStateException("Booking has already been assigned.");

        if (booking.getCreatedAt() == null
                || !LocalDateTime.now().isBefore(booking.getCreatedAt().plusMinutes(30))) {
            throw new IllegalStateException("This booking offer has expired.");
        }

        if (!bookingOfferRepository.existsByBooking_BookingIdAndWorker_UserId(
                bookingId, workerId)) {
            throw new IllegalStateException("This booking has not been offered to you.");
        }

        if (workerBookingRejectionRepository.existsByWorker_UserIdAndBooking_BookingId(
                workerId, bookingId)) {
            throw new IllegalStateException("You have already rejected this booking.");
        }

        Worker worker = workerRepository.findByUserIdForUpdate(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));

        if (!worker.getUser().isActive())
            throw new IllegalStateException("Worker account is inactive.");

        if (worker.getVerificationStatus() != VerificationStatus.VERIFIED)
            throw new IllegalStateException("Only verified workers can accept bookings.");

        if (worker.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE)
            throw new IllegalStateException("Worker is not available.");

        if (!WorkerLocationFreshness.isFresh(worker, LocalDateTime.now(locationClock)))
            throw new IllegalStateException("Share a fresh location before accepting a booking.");

        boolean provides = workerServiceRepository.findByWorker_UserId(workerId).stream()
                .anyMatch(ws -> ws.getService().getServiceId().equals(booking.getService().getServiceId()));

        if (!provides)
            throw new IllegalStateException("Worker does not provide this service.");

        if (bookingRepository.existsByWorkerUserIdAndStatusIn(workerId,
                List.of(
                        BookingStatus.ACCEPTED,
                        BookingStatus.IN_PROGRESS,
                        BookingStatus.WAITING_FOR_PAYMENT)))
            throw new IllegalStateException(
                    "You can't accept another booking until you finish your current booking.");

        if (!LocalDateTime.now().isBefore(booking.getCreatedAt().plusMinutes(30))) {
            throw new IllegalStateException("This booking offer has expired.");
        }

        booking.setWorker(worker);
        booking.setStatus(BookingStatus.ACCEPTED);

        booking.setCompletionOtp(null);
        booking.setCompletionOtpRequestedAt(null);

        booking.setWorkerAcceptanceLatitude(request.getLatitude());
        booking.setWorkerAcceptanceLongitude(request.getLongitude());
        booking.setAcceptedAt(LocalDateTime.now());

        worker.setAvailabilityStatus(AvailabilityStatus.BUSY);

        return toDto(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<WorkerBookingRequestDto> getWorkerBookingRequests(UUID workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));

        if (!worker.getUser().isActive()
                || worker.getVerificationStatus() != VerificationStatus.VERIFIED
                || worker.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE
                || !WorkerLocationFreshness.isFresh(worker, LocalDateTime.now(locationClock))
                || bookingRepository.existsByWorkerUserIdAndStatusIn(
                        workerId,
                        List.of(
                                BookingStatus.ACCEPTED,
                                BookingStatus.IN_PROGRESS,
                                BookingStatus.WAITING_FOR_PAYMENT))) {
            return List.of();
        }

        LocalDateTime requestNow = LocalDateTime.now();
        return bookingOfferRepository
                .findPendingOffersForWorker(workerId, BookingStatus.PENDING)
                .stream()
                .map(offer -> offer.getBooking())
                .filter(booking -> booking.getCreatedAt() != null
                        && requestNow.isBefore(booking.getCreatedAt().plusMinutes(30)))
                .map(b -> new WorkerBookingRequestDto(
                        b.getBookingId(),
                        b.getCustomer().getUserId(),
                        b.getService().getServiceId(),
                        b.getCustomer().getUser().getFullName(),
                        b.getService().getServiceName(),
                        b.getCustomerNote(),
                        b.getStatus(),
                        b.getCreatedAt(),
                        b.getCustomer().getUser().getAddressLine1(),
                        b.getCustomer().getUser().getCity()))
                .toList();
    }

    @Transactional
    public void rejectBooking(UUID workerId, UUID bookingId) {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));

        if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException(
                    "Only verified workers can reject bookings.");
        }

        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Booking is no longer available.");
        }

        if (booking.getCreatedAt() == null
                || !LocalDateTime.now().isBefore(booking.getCreatedAt().plusMinutes(30))) {
            throw new IllegalStateException("This booking offer has expired.");
        }

        if (!bookingOfferRepository.existsByBooking_BookingIdAndWorker_UserId(
                bookingId, workerId)) {
            throw new IllegalStateException("This booking has not been offered to you.");
        }

        boolean provides = workerServiceRepository
                .findByWorker_UserId(workerId)
                .stream()
                .anyMatch(ws -> ws.getService()
                        .getServiceId()
                        .equals(booking.getService().getServiceId()));

        if (!provides) {
            throw new IllegalStateException(
                    "Worker does not provide this service.");
        }

        if (workerBookingRejectionRepository
                .existsByWorker_UserIdAndBooking_BookingId(
                        workerId,
                        bookingId)) {

            return;
        }

        WorkerBookingRejection rejection = new WorkerBookingRejection();

        rejection.setWorker(worker);
        rejection.setBooking(booking);

        workerBookingRejectionRepository.save(rejection);
    }

    @Transactional
    public BookingDto startBooking(
            UUID workerId,
            UUID bookingId,
            String otp) {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));

        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (booking.getWorker() == null
                || !booking.getWorker().getUserId().equals(workerId)) {

            throw new IllegalStateException(
                    "Booking is not assigned to this worker.");
        }

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "Booking cannot be started.");
        }

        if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException(
                    "Only verified workers can start bookings.");
        }

        if (otp == null || !otp.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "Please enter a valid 4-digit OTP.");
        }

        if (booking.getStartOtp() == null
                || !booking.getStartOtp().equals(otp)) {

            throw new IllegalArgumentException(
                    "Incorrect OTP. Please ask the customer for the correct Start OTP.");
        }

        booking.setStartOtp(null);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setStartedAt(LocalDateTime.now());

        return toDto(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto workerCancelBooking(
            UUID workerId,
            UUID bookingId,
            WorkerCancellationRequestDto request) {

        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        // Make sure this booking belongs to the logged-in worker
        if (booking.getWorker() == null
                || !booking.getWorker().getUserId().equals(workerId)) {

            throw new IllegalStateException(
                    "Booking is not assigned to this worker.");
        }

        Worker worker = workerRepository.findByUserIdForUpdate(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));

        if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException(
                    "Only verified workers can cancel bookings.");
        }

        String reason = request.getReason();

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Cancellation reason is required.");
        }

        String message = request.getMessage();

        if ("OTHER".equals(reason)
                && (message == null || message.isBlank())) {

            throw new IllegalArgumentException(
                    "Please provide a message for the selected reason.");
        }

        /*
         * Worker cancellation before starting the service.
         */
        if (booking.getStatus() == BookingStatus.ACCEPTED) {

            validateAcceptedCancellationReason(reason);

            booking.setStatus(BookingStatus.WORKER_CANCELLED);

        }

        /*
         * Worker cannot complete after the service has started.
         */
        else if (booking.getStatus() == BookingStatus.IN_PROGRESS) {

            validateInProgressCancellationReason(reason);

            booking.setStatus(BookingStatus.WORKER_CANNOT_COMPLETE);

        }

        else {

            throw new IllegalStateException(
                    "This booking cannot be cancelled by the worker.");
        }

        booking.setWorkerCancelledAt(LocalDateTime.now());
        booking.setWorkerCancellationReason(reason);
        booking.setWorkerCancellationMessage(
                message == null || message.isBlank()
                        ? null
                        : message.trim());

        /*
         * The worker is no longer handling this booking,
         * so make the worker available again.
         */

        worker.markAvailableAfterBooking();

        return toDto(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto completeBooking(
            UUID workerId,
            UUID bookingId,
            String otp) {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));

        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (booking.getWorker() == null
                || !booking.getWorker().getUserId().equals(workerId)) {

            throw new IllegalStateException(
                    "Booking is not assigned to this worker.");
        }

        if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException(
                    "Only verified workers can complete bookings.");
        }

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Booking is not in progress.");
        }

        if (!booking.isCompletionRequested()) {
            throw new IllegalStateException(
                    "Completion has not been requested yet. Ask the customer for the Completion OTP only after requesting completion.");
        }

        if (otp == null || !otp.matches("\\d{4}")) {
            throw new IllegalArgumentException(
                    "Please enter a valid 4-digit Completion OTP.");
        }

        if (booking.getCompletionOtp() == null
                || !booking.getCompletionOtp().equals(otp)) {

            throw new IllegalArgumentException(
                    "Incorrect Completion OTP. Please ask the customer for the correct OTP.");
        }

        // Record when the worker completed the service.
        booking.setWorkerCompletedAt(LocalDateTime.now());

        booking.setStatus(BookingStatus.WAITING_FOR_PAYMENT);

        booking.setCompletionOtp(null);

        booking.setCompletionRequested(false);

        // Create the final bill after OTP verification.
        billingService.createBilling(booking);

        return toDto(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto requestCompletion(UUID workerId, UUID bookingId) {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));

        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (booking.getWorker() == null
                || !booking.getWorker().getUserId().equals(workerId)) {

            throw new IllegalStateException(
                    "Booking is not assigned to this worker.");
        }

        if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException(
                    "Only verified workers can request completion.");
        }

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Only an in-progress booking can be completed.");
        }

        List<BookingPart> parts = bookingPartRepository.findByBookingBookingId(bookingId);

        boolean hasPendingParts = parts.stream()
                .anyMatch(part -> part.getStatus() == PartApprovalStatus.PENDING);

        if (hasPendingParts) {
            throw new IllegalStateException(
                    "All added parts must be approved by the customer before completion can be requested.");
        }

        if (booking.isCompletionRequested()) {
            throw new IllegalStateException(
                    "Completion has already been requested. Please obtain the Completion OTP from the customer.");
        }

        String completionOtp = generateOtp();

        booking.setCompletionOtp(completionOtp);
        booking.setCompletionOtpRequestedAt(LocalDateTime.now());
        booking.setCompletionRequested(true);

        return toDto(bookingRepository.save(booking));
    }

    public List<BookingDto> getCustomerBookings(UUID customerId) {

        return bookingRepository.findByCustomerUserId(customerId)
                .stream()
                .map(this::toCustomerDto)
                .toList();
    }

    public List<BookingDto> getWorkerBookings(UUID workerId) {
        return bookingRepository.findByWorkerUserId(workerId).stream().map(this::toDto).toList();
    }

    private void markWorkerAvailableAfterBooking(Booking booking) {
        if (booking.getWorker() == null) {
            return;
        }

        UUID workerId = booking.getWorker().getUserId();
        Worker worker = workerRepository.findByUserIdForUpdate(workerId)
                .orElseThrow(() -> new IllegalStateException("Assigned worker not found."));
        worker.markAvailableAfterBooking();
    }

    private BookingDto toDto(Booking b) {
        BookingDto d = new BookingDto();
        d.setBookingId(b.getBookingId());
        d.setCustomerId(b.getCustomer().getUserId());
        d.setServiceId(b.getService().getServiceId());
        d.setServiceName(b.getService().getServiceName());
        if (b.getWorker() != null) {
            d.setWorkerId(b.getWorker().getUserId());
            d.setWorkerName(b.getWorker().getUser().getFullName());
        }
        d.setStatus(b.getStatus());
        d.setCustomerNote(b.getCustomerNote());
        d.setCreatedAt(b.getCreatedAt());
        d.setUpdatedAt(b.getUpdatedAt());
        d.setAcceptedAt(b.getAcceptedAt());
        d.setStartedAt(b.getStartedAt());
        d.setCompletedAt(b.getCompletedAt());
        d.setWorkerCompletedAt(b.getWorkerCompletedAt());
        d.setCancelledAt(b.getCancelledAt());
        d.setFailedAt(b.getFailedAt());
        d.setWorkerCancelledAt(b.getWorkerCancelledAt());
        d.setWorkerCancellationReason(b.getWorkerCancellationReason());
        d.setWorkerCancellationMessage(b.getWorkerCancellationMessage());
        d.setCompletionRequested(b.isCompletionRequested());
        d.setCustomerName(b.getCustomer().getUser().getFullName());
        d.setCustomerPhone(b.getCustomer().getUser().getPhone());
        d.setCustomerAddressLine1(b.getCustomer().getUser().getAddressLine1());
        d.setCustomerAddressLine2(b.getCustomer().getUser().getAddressLine2());
        d.setCustomerLandmark(b.getCustomer().getUser().getLandmark());
        d.setCustomerCity(b.getCustomer().getUser().getCity());
        d.setCustomerState(b.getCustomer().getUser().getState());
        d.setCustomerPinCode(b.getCustomer().getUser().getPinCode());

        // Include the final billed amount, if a bill exists.
        billingRepository.findByBookingBookingId(b.getBookingId())
                .ifPresent(billing ->
                        d.setFinalBilledPrice(billing.getFinalBilledPrice()));

        // Include the latest payment details, if a payment exists.
        paymentRepository.findFirstByBookingBookingIdOrderByCreatedAtDesc(
                b.getBookingId()
        ).ifPresent(payment -> {
            d.setPaymentStatus(payment.getStatus());
            d.setPaymentMethod(payment.getPaymentMethod());
        });

        return d;
    }

    private BookingDto toCustomerDto(Booking b) {

        BookingDto d = toDto(b);

        // Start OTP is available only while the booking is accepted.
        if (b.getStatus() == BookingStatus.ACCEPTED) {
            d.setStartOtp(b.getStartOtp());
        }

        // Completion OTP is available only after the worker
        // has explicitly requested completion.
        if (b.getStatus() == BookingStatus.IN_PROGRESS
                && b.isCompletionRequested()) {

            d.setCompletionOtp(b.getCompletionOtp());
            d.setCompletionRequested(true);
        }

        return d;
    }

    private void validateAcceptedCancellationReason(String reason) {

        switch (reason) {

            case "EMERGENCY_PERSONAL_ISSUE":
            case "VEHICLE_TRANSPORT_PROBLEM":
            case "UNABLE_TO_REACH_CUSTOMER":
            case "INCORRECT_BOOKING_SERVICE_INFORMATION":
            case "OTHER":
                return;

            default:
                throw new IllegalArgumentException(
                        "Invalid cancellation reason for an accepted booking.");
        }
    }

    private void validateInProgressCancellationReason(String reason) {

        switch (reason) {

            case "CANNOT_SOLVE_PROBLEM":
            case "REQUIRES_DIFFERENT_EXPERTISE":
            case "REQUIRED_EQUIPMENT_UNAVAILABLE":
            case "REQUIRED_PART_MATERIAL_UNAVAILABLE":
            case "OTHER":
                return;

            default:
                throw new IllegalArgumentException(
                        "Invalid cancellation reason for an in-progress booking.");
        }
    }

    private String generateOtp() {
        return String.format(
                "%04d",
                ThreadLocalRandom.current().nextInt(0, 10000));
    }
}
