package com.servicehub.serv.service;

import com.servicehub.serv.dto.WorkerLocationUpdateDto;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.exception.AccountStatusException;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkerStatusServiceTest {

    private final UUID workerId = UUID.randomUUID();
    private final WorkerRepository workerRepository = mock(WorkerRepository.class);
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-11-12T12:00:00Z"),
            ZoneOffset.UTC);
    private final WorkerStatusService workerStatusService =
            createWorkerStatusService();

    private Worker worker;

    @BeforeEach
    void setUp() {
        Users user = new Users();
        user.setUserId(workerId);
        user.setActive(true);

        worker = new Worker();
        worker.setUserId(workerId);
        worker.setUser(user);
        worker.setVerificationStatus(VerificationStatus.VERIFIED);
        worker.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);

        when(workerRepository.findByUserIdForUpdate(workerId))
                .thenReturn(Optional.of(worker));
        when(bookingRepository.existsByWorkerUserIdAndStatusIn(
                workerId,
                List.of(
                        BookingStatus.ACCEPTED,
                        BookingStatus.IN_PROGRESS,
                        BookingStatus.WAITING_FOR_PAYMENT)))
                .thenReturn(false);
    }

    @Test
    void verifiedWorkerCanGoOnline() {
        assertEquals(
                AvailabilityStatus.AVAILABLE,
                workerStatusService.goOnline(workerId).getAvailabilityStatus());

        verify(workerRepository).save(worker);
    }

    @Test
    void verifiedWorkerCanGoOffline() {
        worker.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        assertEquals(
                AvailabilityStatus.UNAVAILABLE,
                workerStatusService.goOffline(workerId).getAvailabilityStatus());

        verify(workerRepository).save(worker);
    }

    @Test
    void unverifiedWorkerCannotGoOnline() {
        worker.setVerificationStatus(VerificationStatus.PENDING);

        assertThrows(
                AccountStatusException.class,
                () -> workerStatusService.goOnline(workerId));

        verify(bookingRepository, never())
                .existsByWorkerUserIdAndStatusIn(eq(workerId), anyList());
        verify(workerRepository, never()).save(worker);
    }

    @Test
    void inactiveWorkerCannotGoOnline() {
        worker.getUser().setActive(false);

        assertThrows(
                AccountStatusException.class,
                () -> workerStatusService.goOnline(workerId));

        verify(workerRepository, never()).save(worker);
    }

    @Test
    void workerWithActiveBookingCannotGoOnline() {
        when(bookingRepository.existsByWorkerUserIdAndStatusIn(
                workerId,
                List.of(
                        BookingStatus.ACCEPTED,
                        BookingStatus.IN_PROGRESS,
                        BookingStatus.WAITING_FOR_PAYMENT)))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> workerStatusService.goOnline(workerId));

        verify(workerRepository, never()).save(worker);
    }

    @Test
    void busyWorkerCannotGoOnlineEvenWithoutAnActiveBookingRecord() {
        worker.setAvailabilityStatus(AvailabilityStatus.BUSY);

        assertThrows(
                IllegalStateException.class,
                () -> workerStatusService.goOnline(workerId));

        verify(workerRepository, never()).save(worker);
    }

    @Test
    void busyWorkerCannotGoOffline() {
        worker.setAvailabilityStatus(AvailabilityStatus.BUSY);

        assertThrows(
                IllegalStateException.class,
                () -> workerStatusService.goOffline(workerId));

        verify(workerRepository, never()).save(worker);
    }

    @Test
    void validOdishaLocationIsStoredWithServerTimestampWithoutChangingAvailability() {
        worker.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        WorkerLocationUpdateDto request = location(20.2961, 85.8245);

        assertEquals(
                AvailabilityStatus.AVAILABLE,
                workerStatusService.updateLocation(workerId, request)
                        .getAvailabilityStatus());
        assertEquals(20.2961, worker.getCurrentLatitude().doubleValue());
        assertEquals(85.8245, worker.getCurrentLongitude().doubleValue());
        assertEquals(
                LocalDateTime.of(2026, 11, 12, 12, 0),
                worker.getLocationLastUpdatedAt());
        verify(workerRepository).save(worker);
    }

    @Test
    void rejectsLatitudeOutsideValidRange() {
        worker.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        assertThrows(
                IllegalArgumentException.class,
                () -> workerStatusService.updateLocation(
                        workerId,
                        location(91, 85.8245)));
        verify(workerRepository, never()).save(worker);
    }

    @Test
    void rejectsLongitudeOutsideValidRange() {
        worker.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        assertThrows(
                IllegalArgumentException.class,
                () -> workerStatusService.updateLocation(
                        workerId,
                        location(20.2961, 181)));
        verify(workerRepository, never()).save(worker);
    }

    @Test
    void rejectsMissingCoordinates() {
        worker.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        assertThrows(
                IllegalArgumentException.class,
                () -> workerStatusService.updateLocation(workerId, new WorkerLocationUpdateDto()));
        verify(workerRepository, never()).save(worker);
    }

    @Test
    void rejectsCoordinatesOutsideOdisha() {
        worker.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        assertThrows(
                IllegalArgumentException.class,
                () -> workerStatusService.updateLocation(
                        workerId,
                        location(22.5726, 88.3639)));
        verify(workerRepository, never()).save(worker);
    }

    @Test
    void rejectsNonFiniteCoordinates() {
        worker.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        WorkerLocationUpdateDto request = location(Double.NaN, 85.8245);

        assertThrows(
                IllegalArgumentException.class,
                () -> workerStatusService.updateLocation(workerId, request));
        verify(workerRepository, never()).save(worker);
    }

    @Test
    void locationUpdateRequiresAvailableWorker() {
        assertThrows(
                IllegalStateException.class,
                () -> workerStatusService.updateLocation(
                        workerId,
                        location(20.2961, 85.8245)));
        verify(workerRepository, never()).save(worker);
    }

    @Test
    void locationUpdateRejectsBusyWorker() {
        worker.setAvailabilityStatus(AvailabilityStatus.BUSY);

        assertThrows(
                IllegalStateException.class,
                () -> workerStatusService.updateLocation(
                        workerId,
                        location(20.2961, 85.8245)));
        verify(workerRepository, never()).save(worker);
    }

    private WorkerStatusService createWorkerStatusService() {
        return new WorkerStatusService(
                workerRepository,
                bookingRepository,
                new OdishaServiceArea(),
                clock);
    }

    private WorkerLocationUpdateDto location(double latitude, double longitude) {
        WorkerLocationUpdateDto request = new WorkerLocationUpdateDto();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        return request;
    }
}
