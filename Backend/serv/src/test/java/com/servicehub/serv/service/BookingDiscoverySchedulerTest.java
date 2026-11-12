package com.servicehub.serv.service;

import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingDiscoverySchedulerTest {

    private BookingRepository bookingRepository;
    private BookingDiscoveryService bookingDiscoveryService;
    private BookingDiscoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        bookingDiscoveryService = mock(BookingDiscoveryService.class);
        scheduler = new BookingDiscoveryScheduler(bookingRepository, bookingDiscoveryService);
    }

    @Test
    void schedulerQueriesForPendingBookingsWithThirtyMinuteWindow() {
        when(bookingRepository.findPendingBookingIdsForDiscovery(
                any(), any(), any(), any()))
                .thenReturn(List.of());

        scheduler.discoverPendingBookings();

        ArgumentCaptor<LocalDateTime> createdAfter = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> createdBefore = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(bookingRepository).findPendingBookingIdsForDiscovery(
                eq(BookingStatus.PENDING),
                createdAfter.capture(),
                createdBefore.capture(),
                any(Pageable.class));

        Duration window = Duration.between(
                createdAfter.getValue(), createdBefore.getValue());
        assertEquals(Duration.ofMinutes(30), window);
    }

    @Test
    void schedulerInvokesDiscoverBookingForEachFoundId() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        when(bookingRepository.findPendingBookingIdsForDiscovery(
                any(), any(), any(), any()))
                .thenReturn(List.of(id1, id2, id3));

        scheduler.discoverPendingBookings();

        verify(bookingDiscoveryService).discoverBooking(id1);
        verify(bookingDiscoveryService).discoverBooking(id2);
        verify(bookingDiscoveryService).discoverBooking(id3);
    }

    @Test
    void schedulerPaginatesThroughAllPendingBookings() {
        // Page size is 200 in the scheduler
        List<UUID> page1 = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            page1.add(UUID.randomUUID());
        }
        List<UUID> page2 = List.of(UUID.randomUUID(), UUID.randomUUID());

        when(bookingRepository.findPendingBookingIdsForDiscovery(
                any(), any(), any(), eq(PageRequest.of(0, 200))))
                .thenReturn(page1);
        when(bookingRepository.findPendingBookingIdsForDiscovery(
                any(), any(), any(), eq(PageRequest.of(1, 200))))
                .thenReturn(page2);

        scheduler.discoverPendingBookings();

        // All 202 bookings should be discovered
        verify(bookingDiscoveryService, times(202)).discoverBooking(any());
    }

    @Test
    void schedulerStopsWhenPageIsUnderPageSize() {
        List<UUID> partialPage = List.of(UUID.randomUUID());

        when(bookingRepository.findPendingBookingIdsForDiscovery(
                any(), any(), any(), eq(PageRequest.of(0, 200))))
                .thenReturn(partialPage);

        scheduler.discoverPendingBookings();

        // Should only query page 0
        verify(bookingRepository, never()).findPendingBookingIdsForDiscovery(
                any(), any(), any(), eq(PageRequest.of(1, 200)));
        verify(bookingDiscoveryService, times(1)).discoverBooking(any());
    }

    @Test
    void schedulerHandlesEmptyFirstPage() {
        when(bookingRepository.findPendingBookingIdsForDiscovery(
                any(), any(), any(), any()))
                .thenReturn(List.of());

        scheduler.discoverPendingBookings();

        verify(bookingDiscoveryService, never()).discoverBooking(any());
    }
}

