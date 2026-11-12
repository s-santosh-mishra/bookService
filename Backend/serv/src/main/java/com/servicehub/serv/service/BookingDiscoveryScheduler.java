package com.servicehub.serv.service;

import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.repository.BookingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class BookingDiscoveryScheduler {

    private static final int PAGE_SIZE = 200;

    private final BookingRepository bookingRepository;
    private final BookingDiscoveryService bookingDiscoveryService;

    public BookingDiscoveryScheduler(
            BookingRepository bookingRepository,
            BookingDiscoveryService bookingDiscoveryService) {
        this.bookingRepository = bookingRepository;
        this.bookingDiscoveryService = bookingDiscoveryService;
    }

    @Scheduled(fixedDelay = 10_000)
    public void discoverPendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAfter = now.minusMinutes(30);
        for (int page = 0; ; page++) {
            List<UUID> bookingIds = bookingRepository.findPendingBookingIdsForDiscovery(
                    BookingStatus.PENDING,
                    createdAfter,
                    now,
                    PageRequest.of(page, PAGE_SIZE));
            bookingIds.forEach(bookingDiscoveryService::discoverBooking);
            if (bookingIds.size() < PAGE_SIZE) {
                return;
            }
        }
    }
}
