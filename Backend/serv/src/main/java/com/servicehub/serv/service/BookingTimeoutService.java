package com.servicehub.serv.service;

import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.repository.BookingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingTimeoutService {

    private final BookingRepository bookingRepository;
    private final BillingService billingService;
    private final BookingPartService bookingPartService;

    public BookingTimeoutService(
            BookingRepository bookingRepository,
            BillingService billingService,
            BookingPartService bookingPartService) {

        this.bookingRepository = bookingRepository;
        this.billingService = billingService;
        this.bookingPartService = bookingPartService;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePendingBookings() {

        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(30);

        List<Booking> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore(
                BookingStatus.PENDING,
                timeoutTime);

        for (Booking booking : expiredBookings) {

            booking.setStatus(BookingStatus.NO_WORKER);
        }

        if (!expiredBookings.isEmpty()) {
            bookingRepository.saveAll(expiredBookings);
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCompleteBookings() {

        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(30);

        List<Booking> expiredBookings = bookingRepository
                .findByStatusAndWorkerConfirmedCompletionTrueAndCustomerConfirmedCompletionFalseAndWorkerCompletedAtBefore(
                        BookingStatus.IN_PROGRESS,
                        timeoutTime);

        for (Booking booking : expiredBookings) {

            booking.setStatus(BookingStatus.AUTO_COMPLETED);
            booking.setCompletedAt(LocalDateTime.now());

            bookingPartService.autoApprovePendingParts(
                    booking.getBookingId());

            billingService.createBilling(booking);

            if (booking.getWorker() != null) {
                booking.getWorker().setAvailabilityStatus(
                        AvailabilityStatus.AVAILABLE);
            }
        }

        if (!expiredBookings.isEmpty()) {
            bookingRepository.saveAll(expiredBookings);
        }
    }
}