package com.servicehub.serv.service;

import com.servicehub.serv.dto.AdminBookingDetailsDto;
import com.servicehub.serv.dto.AdminBookingPaymentDto;
import com.servicehub.serv.dto.AdminBookingSummaryDto;
import com.servicehub.serv.entity.Billing;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.exception.BookingConflictException;
import com.servicehub.serv.exception.ResourceNotFoundException;
import com.servicehub.serv.repository.BillingRepository;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.PaymentRepository;
import com.servicehub.serv.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;
    private final WorkerRepository workerRepository;

    public AdminBookingService(
            BookingRepository bookingRepository,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            WorkerRepository workerRepository) {
        this.bookingRepository = bookingRepository;
        this.billingRepository = billingRepository;
        this.paymentRepository = paymentRepository;
        this.workerRepository = workerRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminBookingSummaryDto> getBookings() {
        List<Booking> bookings = bookingRepository.findAllForAdmin();

        if (bookings.isEmpty()) {
            return List.of();
        }

        List<UUID> bookingIds = bookings.stream()
                .map(booking -> booking.getBookingId())
                .toList();

        Map<UUID, Billing> billings = billingRepository
                .findByBookingBookingIdIn(bookingIds)
                .stream()
                .collect(Collectors.toMap(
                        billing -> billing.getBooking().getBookingId(),
                        Function.identity()));

        Map<UUID, List<Payment>> payments = groupPayments(bookingIds);

        return bookings.stream()
                .map(booking -> toSummary(
                        booking,
                        billings.get(booking.getBookingId()),
                        payments.getOrDefault(booking.getBookingId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminBookingDetailsDto getBooking(UUID bookingId) {
        Booking booking = findBookingForAdmin(bookingId);
        Billing billing = billingRepository
                .findByBookingBookingId(bookingId)
                .orElse(null);
        List<Payment> payments = paymentRepository
                .findByBookingBookingIdInOrderByCreatedAtDesc(List.of(bookingId));

        return toDetails(booking, billing, payments);
    }

    @Transactional
    public AdminBookingDetailsDto cancelBooking(
            UUID bookingId,
            String cancellationReason) {
        String reason = cancellationReason == null
                ? ""
                : cancellationReason.trim();

        if (reason.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cancellation reason is required.");
        }

        if (reason.length() > 1000) {
            throw new IllegalArgumentException(
                    "Cancellation reason cannot exceed 1000 characters.");
        }

        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found."));

        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.ACCEPTED
                && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new BookingConflictException(
                    "This booking cannot be cancelled in its current status.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setAdminCancellationReason(reason);

        if (booking.getWorker() != null) {
            Worker worker = workerRepository.findByUserIdForUpdate(
                            booking.getWorker().getUserId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Assigned worker not found."));
            worker.markAvailableAfterBooking();
        }

        Booking savedBooking = bookingRepository.save(booking);
        Billing billing = billingRepository
                .findByBookingBookingId(bookingId)
                .orElse(null);
        List<Payment> payments = paymentRepository
                .findByBookingBookingIdInOrderByCreatedAtDesc(List.of(bookingId));

        return toDetails(savedBooking, billing, payments);
    }

    private Booking findBookingForAdmin(UUID bookingId) {
        return bookingRepository.findByBookingIdForAdmin(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found."));
    }

    private Map<UUID, List<Payment>> groupPayments(
            Collection<UUID> bookingIds) {
        return paymentRepository
                .findByBookingBookingIdInOrderByCreatedAtDesc(bookingIds)
                .stream()
                .collect(Collectors.groupingBy(
                        payment -> payment.getBooking().getBookingId()));
    }

    private AdminBookingSummaryDto toSummary(
            Booking booking,
            Billing billing,
            List<Payment> payments) {
        Users customer = booking.getCustomer().getUser();
        Users worker = booking.getWorker() == null
                ? null
                : booking.getWorker().getUser();

        return new AdminBookingSummaryDto(
                booking.getBookingId(),
                customer.getUserId(),
                customer.getFullName(),
                customer.getPhone(),
                worker == null ? null : worker.getUserId(),
                worker == null ? null : worker.getFullName(),
                worker == null ? null : worker.getPhone(),
                booking.getService().getServiceId(),
                booking.getService().getServiceName(),
                booking.getService().getCategory().getCategoryName(),
                booking.getStatus(),
                booking.getCreatedAt(),
                billing == null ? null : billing.getFinalBilledPrice(),
                payments.stream()
                        .map(payment -> payment.getPaymentMethod())
                        .distinct()
                        .toList(),
                payments.stream()
                        .map(payment -> payment.getStatus())
                        .distinct()
                        .toList());
    }

    private AdminBookingDetailsDto toDetails(
            Booking booking,
            Billing billing,
            List<Payment> payments) {
        Users customer = booking.getCustomer().getUser();
        Users worker = booking.getWorker() == null
                ? null
                : booking.getWorker().getUser();

        AdminBookingDetailsDto.Billing billingDetails = billing == null
                ? null
                : new AdminBookingDetailsDto.Billing(
                        billing.getBasePricePerHour(),
                        billing.getMinimumServiceFee(),
                        billing.getLabourCharge(),
                        billing.getTravelCharge(),
                        billing.getPartsCost(),
                        billing.getAdditionalFees(),
                        billing.getFinalBilledPrice(),
                        billing.getBillableHours());

        return new AdminBookingDetailsDto(
                toSummary(booking, billing, payments),
                customer.getCredentials().getEmail(),
                new AdminBookingDetailsDto.Address(
                        customer.getAddressLine1(),
                        customer.getAddressLine2(),
                        customer.getLandmark(),
                        customer.getCity(),
                        customer.getState(),
                        customer.getPinCode()),
                worker == null ? null : worker.getCredentials().getEmail(),
                booking.getServiceLatitude(),
                booking.getServiceLongitude(),
                booking.getCustomerNote(),
                new AdminBookingDetailsDto.Lifecycle(
                        booking.getCreatedAt(),
                        booking.getUpdatedAt(),
                        booking.getAcceptedAt(),
                        booking.getStartedAt(),
                        booking.getCompletedAt(),
                        booking.getWorkerCompletedAt(),
                        booking.getCancelledAt(),
                        booking.getFailedAt(),
                        booking.getWorkerCancelledAt(),
                        booking.getCompletionOtpRequestedAt()),
                billingDetails,
                booking.getAdminCancellationReason(),
                booking.getWorkerCancellationReason(),
                booking.getWorkerCancellationMessage(),
                payments.stream()
                        .map(payment -> new AdminBookingPaymentDto(
                                payment.getPaymentId(),
                                payment.getPaymentMethod(),
                                payment.getStatus(),
                                payment.getAmount(),
                                payment.getRazorpayOrderId(),
                                payment.getRazorpayPaymentId(),
                                payment.getCreatedAt(),
                                payment.getPaidAt()))
                        .toList());
    }
}
