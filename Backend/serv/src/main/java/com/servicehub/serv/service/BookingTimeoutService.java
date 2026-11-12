package com.servicehub.serv.service;

import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.repository.BookingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BookingTimeoutService {

    private final BookingRepository bookingRepository;

    public BookingTimeoutService(
            BookingRepository bookingRepository) {

        this.bookingRepository = bookingRepository;
    }

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        bookingRepository.expireUnassignedPendingBookings(
                BookingStatus.PENDING,
                BookingStatus.NO_WORKER,
                now.minusMinutes(30),
                now);
    }
}