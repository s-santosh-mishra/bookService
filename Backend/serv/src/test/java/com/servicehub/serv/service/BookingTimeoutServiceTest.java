package com.servicehub.serv.service;

import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BookingTimeoutServiceTest {

    @Test
    void expiryUsesConditionalUnassignedPendingUpdateAtThirtyMinutes() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingTimeoutService service = new BookingTimeoutService(bookingRepository);
        ArgumentCaptor<LocalDateTime> deadline = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> updatedAt = ArgumentCaptor.forClass(LocalDateTime.class);

        service.expirePendingBookings();

        verify(bookingRepository).expireUnassignedPendingBookings(
                eq(BookingStatus.PENDING),
                eq(BookingStatus.NO_WORKER),
                deadline.capture(),
                updatedAt.capture());
        assertEquals(
                Duration.ofMinutes(30),
                Duration.between(deadline.getValue(), updatedAt.getValue()));
    }
}
