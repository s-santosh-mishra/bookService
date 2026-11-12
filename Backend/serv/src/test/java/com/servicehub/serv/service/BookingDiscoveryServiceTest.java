package com.servicehub.serv.service;

import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.repository.BookingOfferRepository;
import com.servicehub.serv.repository.BookingRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingDiscoveryServiceTest {

    @Test
    void discoveryRadiusExpandsAtThreeMinuteIntervals() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);

        assertEquals(2.0, BookingDiscoveryService.discoveryRadiusKm(now, now));
        assertEquals(2.0, BookingDiscoveryService.discoveryRadiusKm(now, now.plusMinutes(2)));
        assertEquals(5.0, BookingDiscoveryService.discoveryRadiusKm(now, now.plusMinutes(3)));
        assertEquals(8.0, BookingDiscoveryService.discoveryRadiusKm(now, now.plusMinutes(6)));
        assertEquals(10.0, BookingDiscoveryService.discoveryRadiusKm(now, now.plusMinutes(9)));
        assertEquals(10.0, BookingDiscoveryService.discoveryRadiusKm(now, now.plusMinutes(25)));
    }

    @Test
    void onlyFreshWorkersWithinCurrentRadiusReceiveOffers() {
        Service serviceEntity = new Service();
        serviceEntity.setServiceId(UUID.randomUUID());
        Booking booking = createBooking(serviceEntity, LocalDateTime.now().minusMinutes(3));
        Worker withinRadius = createWorker(20.04, 85.0, LocalDateTime.now(Clock.systemUTC()));
        Worker outsideRadius = createWorker(20.06, 85.0, LocalDateTime.now(Clock.systemUTC()));
        Worker stale = createWorker(20.01, 85.0, LocalDateTime.now(Clock.systemUTC()).minusMinutes(4));

        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerServiceRepository workerServiceRepository = mock(WorkerServiceRepository.class);
        BookingOfferRepository bookingOfferRepository = mock(BookingOfferRepository.class);
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(workerServiceRepository.findEligibleWorkersForOffer(
                any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(withinRadius, outsideRadius, stale));

        BookingDiscoveryService discoveryService = new BookingDiscoveryService(
                bookingRepository,
                workerServiceRepository,
                bookingOfferRepository,
                Clock.systemUTC());

        assertEquals(1, discoveryService.discoverBooking(booking.getBookingId()));
        verify(bookingOfferRepository).saveAll(any());
    }

    @Test
    void expiredOrNonPendingBookingDoesNotRunDiscovery() {
        Service serviceEntity = new Service();
        serviceEntity.setServiceId(UUID.randomUUID());
        Booking booking = createBooking(serviceEntity, LocalDateTime.now().minusMinutes(30));
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WorkerServiceRepository workerServiceRepository = mock(WorkerServiceRepository.class);
        BookingOfferRepository bookingOfferRepository = mock(BookingOfferRepository.class);
        when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                .thenReturn(Optional.of(booking));

        BookingDiscoveryService discoveryService = new BookingDiscoveryService(
                bookingRepository,
                workerServiceRepository,
                bookingOfferRepository,
                Clock.systemUTC());

        assertEquals(0, discoveryService.discoverBooking(booking.getBookingId()));
        verify(workerServiceRepository, org.mockito.Mockito.never())
                .findEligibleWorkersForOffer(
                        any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    private Booking createBooking(Service service, LocalDateTime createdAt) {
        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setService(service);
        booking.setStatus(BookingStatus.PENDING);
        booking.setServiceLatitude(BigDecimal.valueOf(20.0));
        booking.setServiceLongitude(BigDecimal.valueOf(85.0));
        setCreatedAt(booking, createdAt);
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

    private Worker createWorker(double latitude, double longitude, LocalDateTime updatedAt) {
        Users user = new Users();
        user.setUserId(UUID.randomUUID());
        user.setActive(true);
        Worker worker = new Worker();
        worker.setUserId(user.getUserId());
        worker.setUser(user);
        worker.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        worker.setVerificationStatus(VerificationStatus.VERIFIED);
        worker.setCurrentLatitude(BigDecimal.valueOf(latitude));
        worker.setCurrentLongitude(BigDecimal.valueOf(longitude));
        worker.setLocationLastUpdatedAt(updatedAt);
        return worker;
    }
}
