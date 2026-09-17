package com.servicehub.serv.service;

import com.servicehub.serv.entity.Booking;
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

    public BookingTimeoutService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePendingBookings() {

        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(30);

        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndCreatedAtBefore(
                        BookingStatus.PENDING,
                        timeoutTime
                );

        for (Booking booking : expiredBookings) {

            booking.setStatus(BookingStatus.NO_WORKER);
        }

        if (!expiredBookings.isEmpty()) {
            bookingRepository.saveAll(expiredBookings);
        }
    }
}