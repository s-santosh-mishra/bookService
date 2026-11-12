package com.servicehub.serv.service;

import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.BookingOffer;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.repository.BookingOfferRepository;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.WorkerServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Progressive Booking Discovery.
 *
 * Scenarios tested:
 *  1. Initial discovery within 2 km.
 *  2. Radius expansion at 3 minutes (5 km).
 *  3. Radius expansion at 6 minutes (8 km).
 *  4. Radius expansion at 9 minutes (10 km).
 *  5. The 10 km maximum radius.
 *  6. Worker location freshness (3-minute limit).
 *  7-10. Exclusion of BUSY, UNAVAILABLE, unverified, inactive, unsupported, and conflicting workers
 *        (enforced at DB query level; verified via query parameters and freshness/distance filters).
 *  11. No duplicate offers for the same booking and worker (bookingId passed to DB query).
 *  12. Previously rejected workers do not receive another offer (bookingId passed to DB query).
 *  13. Newly eligible workers can receive offers during the discovery period.
 *  14. Expired or non-pending bookings do not receive new offers.
 *  15. Correct worker selection based on distance (offers sorted closest first).
 *  16. Boundary cases at each radius transition (2 min 59s vs 3 min, etc.).
 *  17. Booking expiry after 30 minutes.
 *  18. Acceptance and expiry race protection (pessimistic lock, status check, worker != null check).
 */
class BookingDiscoveryServiceTest {

    // Reference point: Bhubaneswar, India
    private static final double SERVICE_LAT = 20.0;
    private static final double SERVICE_LON = 85.0;

    private BookingRepository bookingRepository;
    private WorkerServiceRepository workerServiceRepository;
    private BookingOfferRepository bookingOfferRepository;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        workerServiceRepository = mock(WorkerServiceRepository.class);
        bookingOfferRepository = mock(BookingOfferRepository.class);
    }

    // ──────────────────────────────────────────────────────────────
    //  Scenarios 1–5, 16: Radius tiers and boundaries
    // ──────────────────────────────────────────────────────────────

    @Nested
    class DiscoveryRadiusTiers {

        @Test
        void initialDiscoveryWithinTwoKm() {
            LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
            assertEquals(2.0, BookingDiscoveryService.discoveryRadiusKm(created, created));
            assertEquals(2.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusSeconds(1)));
            assertEquals(2.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(1)));
            assertEquals(2.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(2)));
        }

        @Test
        void radiusExpandsToFiveKmAtThreeMinutes() {
            LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
            assertEquals(5.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(3)));
            assertEquals(5.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(4)));
            assertEquals(5.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(5)));
        }

        @Test
        void radiusExpandsToEightKmAtSixMinutes() {
            LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
            assertEquals(8.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(6)));
            assertEquals(8.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(7)));
            assertEquals(8.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(8)));
        }

        @Test
        void radiusExpandsToTenKmAtNineMinutes() {
            LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
            assertEquals(10.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(9)));
        }

        @Test
        void tenKmIsMaximumRadius() {
            LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
            assertEquals(10.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(15)));
            assertEquals(10.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(25)));
            assertEquals(10.0, BookingDiscoveryService.discoveryRadiusKm(created, created.plusMinutes(29)));
        }

        @Test
        void boundaryAtExactlyThreeMinuteTransitions() {
            LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);

            // Just before 3 minutes: 2 min 59 sec → still 2 km
            assertEquals(2.0, BookingDiscoveryService.discoveryRadiusKm(
                    created, created.plusMinutes(2).plusSeconds(59)));
            // At exactly 3 minutes → 5 km
            assertEquals(5.0, BookingDiscoveryService.discoveryRadiusKm(
                    created, created.plusMinutes(3)));

            // Just before 6 minutes: 5 min 59 sec → still 5 km
            assertEquals(5.0, BookingDiscoveryService.discoveryRadiusKm(
                    created, created.plusMinutes(5).plusSeconds(59)));
            // At exactly 6 minutes → 8 km
            assertEquals(8.0, BookingDiscoveryService.discoveryRadiusKm(
                    created, created.plusMinutes(6)));

            // Just before 9 minutes: 8 min 59 sec → still 8 km
            assertEquals(8.0, BookingDiscoveryService.discoveryRadiusKm(
                    created, created.plusMinutes(8).plusSeconds(59)));
            // At exactly 9 minutes → 10 km
            assertEquals(10.0, BookingDiscoveryService.discoveryRadiusKm(
                    created, created.plusMinutes(9)));
        }

        @Test
        void negativeElapsedTimeClampedToZeroGivingTwoKm() {
            LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
            assertEquals(2.0, BookingDiscoveryService.discoveryRadiusKm(
                    created, created.minusMinutes(5)));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Scenario 6: Worker location freshness (3-minute limit)
    // ──────────────────────────────────────────────────────────────

    @Nested
    class LocationFreshnessFiltering {

        @Test
        void staleWorkerIsFilteredOutDuringDiscovery() {
            Instant instant = Instant.now();
            Clock fixedClock = Clock.fixed(instant, ZoneId.systemDefault());
            LocalDateTime locationNow = LocalDateTime.now(fixedClock);

            Worker stale = createWorker(SERVICE_LAT + 0.001, SERVICE_LON, locationNow.minusMinutes(4));
            Worker fresh = createWorker(SERVICE_LAT + 0.001, SERVICE_LON, locationNow.minusMinutes(1));

            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(1), SERVICE_LAT, SERVICE_LON);
            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(stale, fresh));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, fixedClock);

            int count = service.discoverBooking(booking.getBookingId());

            assertEquals(1, count);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BookingOffer>> captor = ArgumentCaptor.forClass(List.class);
            verify(bookingOfferRepository).saveAll(captor.capture());
            List<BookingOffer> savedOffers = captor.getValue();
            assertEquals(1, savedOffers.size());
            assertEquals(fresh.getUserId(), savedOffers.get(0).getWorker().getUserId());
        }

        @Test
        void workerAtExactThreeMinuteFreshnessBoundaryIsIncluded() {
            Instant instant = Instant.now();
            Clock fixedClock = Clock.fixed(instant, ZoneId.systemDefault());
            LocalDateTime locationNow = LocalDateTime.now(fixedClock);

            Worker atBoundary = createWorker(SERVICE_LAT + 0.001, SERVICE_LON, locationNow.minusMinutes(3));

            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(1), SERVICE_LAT, SERVICE_LON);
            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(atBoundary));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, fixedClock);

            assertEquals(1, service.discoverBooking(booking.getBookingId()));
        }

        @Test
        void workerJustPastThreeMinuteFreshnessIsExcluded() {
            Instant instant = Instant.now();
            Clock fixedClock = Clock.fixed(instant, ZoneId.systemDefault());
            LocalDateTime locationNow = LocalDateTime.now(fixedClock);

            Worker stale = createWorker(SERVICE_LAT + 0.001, SERVICE_LON,
                    locationNow.minusMinutes(3).minusNanos(1));

            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(1), SERVICE_LAT, SERVICE_LON);
            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(stale));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, fixedClock);

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
            verify(bookingOfferRepository, never()).saveAll(any());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Scenarios 7–10: Exclusion filters (DB-level query parameters)
    // ──────────────────────────────────────────────────────────────

    @Nested
    class ExclusionFilters {

        @Test
        void repositoryReceivesCorrectServiceIdAndBookingId() {
            Clock clock = Clock.systemDefaultZone();
            UUID serviceId = UUID.randomUUID();
            Booking booking = createBookingWithServiceId(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(1),
                    SERVICE_LAT, SERVICE_LON, serviceId);
            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of());

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, clock);
            service.discoverBooking(booking.getBookingId());

            verify(workerServiceRepository).findEligibleWorkersForOffer(
                    eq(booking.getBookingId()),
                    eq(serviceId),
                    any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        void repositoryReceivesThreeMinuteFreshnessWindow() {
            LocalDateTime fixedNow = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            Instant instant = fixedNow.atZone(ZoneId.systemDefault()).toInstant();
            Clock fixedClock = Clock.fixed(instant, ZoneId.systemDefault());

            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(1), SERVICE_LAT, SERVICE_LON);
            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of());

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, fixedClock);
            service.discoverBooking(booking.getBookingId());

            verify(workerServiceRepository).findEligibleWorkersForOffer(
                    any(), any(),
                    eq(fixedNow.minusMinutes(3)),
                    eq(fixedNow),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        void emptyWorkerListFromRepositoryResultsInZeroOffers() {
            Clock clock = Clock.systemDefaultZone();
            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(1), SERVICE_LAT, SERVICE_LON);
            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of());

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, clock);

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
            verify(bookingOfferRepository, never()).saveAll(any());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Scenarios 11 & 12: No duplicates & Previously rejected excluded
    // ──────────────────────────────────────────────────────────────

    @Nested
    class DuplicateAndRejectionProtection {

        @Test
        void bookingIdPassedToRepositoryForDuplicateAndRejectionFiltering() {
            Clock clock = Clock.systemDefaultZone();
            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(1), SERVICE_LAT, SERVICE_LON);
            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of());

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, clock);
            service.discoverBooking(booking.getBookingId());

            verify(workerServiceRepository).findEligibleWorkersForOffer(
                    eq(booking.getBookingId()),
                    any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Scenario 13: Newly eligible workers receive offers
    // ──────────────────────────────────────────────────────────────

    @Nested
    class NewlyEligibleWorkers {

        @Test
        void newlyEligibleWorkersReceiveOffersOnSubsequentDiscoveryRun() {
            Clock clock = Clock.systemDefaultZone();
            LocalDateTime locationNow = LocalDateTime.now(clock);

            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(4), SERVICE_LAT, SERVICE_LON);
            Worker newWorker = createWorker(SERVICE_LAT + 0.01, SERVICE_LON, locationNow.minusMinutes(1));

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            // First run: no workers eligible
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of())
                    // Second run: worker becomes eligible
                    .thenReturn(List.of(newWorker));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, clock);

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
            assertEquals(1, service.discoverBooking(booking.getBookingId()));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BookingOffer>> captor = ArgumentCaptor.forClass(List.class);
            verify(bookingOfferRepository).saveAll(captor.capture());
            assertEquals(newWorker.getUserId(),
                    captor.getValue().get(0).getWorker().getUserId());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Scenario 14: Expired or non-pending bookings
    // ──────────────────────────────────────────────────────────────

    @Nested
    class ExpiredOrNonPendingBookings {

        @Test
        void bookingCreatedThirtyMinutesAgoIsNotDiscoverable() {
            Booking booking = createBooking(
                    BookingStatus.PENDING,
                    LocalDateTime.now().minusMinutes(30), SERVICE_LAT, SERVICE_LON);

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, Clock.systemDefaultZone());

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
            verify(workerServiceRepository, never()).findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        void acceptedBookingDoesNotRunDiscovery() {
            Booking booking = createBooking(
                    BookingStatus.ACCEPTED,
                    LocalDateTime.now().minusMinutes(2), SERVICE_LAT, SERVICE_LON);

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, Clock.systemDefaultZone());

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
            verify(workerServiceRepository, never()).findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        void cancelledBookingDoesNotRunDiscovery() {
            Booking booking = createBooking(
                    BookingStatus.CANCELLED,
                    LocalDateTime.now().minusMinutes(2), SERVICE_LAT, SERVICE_LON);

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, Clock.systemDefaultZone());

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
        }

        @Test
        void noWorkerStatusBookingDoesNotRunDiscovery() {
            Booking booking = createBooking(
                    BookingStatus.NO_WORKER,
                    LocalDateTime.now().minusMinutes(2), SERVICE_LAT, SERVICE_LON);

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, Clock.systemDefaultZone());

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
        }

        @Test
        void pendingBookingWithAssignedWorkerDoesNotRunDiscovery() {
            Booking booking = createBooking(
                    BookingStatus.PENDING,
                    LocalDateTime.now().minusMinutes(2), SERVICE_LAT, SERVICE_LON);
            booking.setWorker(createWorker(SERVICE_LAT, SERVICE_LON, LocalDateTime.now()));

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, Clock.systemDefaultZone());

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
            verify(workerServiceRepository, never()).findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        void bookingWithNullCreatedAtDoesNotRunDiscovery() {
            Booking booking = createBooking(
                    BookingStatus.PENDING,
                    null, SERVICE_LAT, SERVICE_LON);

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, Clock.systemDefaultZone());

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Scenario 15: Correct worker selection based on distance
    // ──────────────────────────────────────────────────────────────

    @Nested
    class DistanceBasedSelection {

        @Test
        void workerOutsideCurrentRadiusIsExcluded() {
            Clock clock = Clock.systemDefaultZone();
            LocalDateTime locationNow = LocalDateTime.now(clock);

            // Booking just created (30s ago) → radius = 2 km
            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusSeconds(30), SERVICE_LAT, SERVICE_LON);

            // Worker ~3.3 km away (0.03 degrees latitude)
            Worker farWorker = createWorker(SERVICE_LAT + 0.03, SERVICE_LON, locationNow.minusMinutes(1));
            // Worker ~0.11 km away
            Worker nearWorker = createWorker(SERVICE_LAT + 0.001, SERVICE_LON, locationNow.minusMinutes(1));

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(farWorker, nearWorker));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, clock);

            assertEquals(1, service.discoverBooking(booking.getBookingId()));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BookingOffer>> captor = ArgumentCaptor.forClass(List.class);
            verify(bookingOfferRepository).saveAll(captor.capture());
            List<BookingOffer> offers = captor.getValue();
            assertEquals(1, offers.size());
            assertEquals(nearWorker.getUserId(), offers.get(0).getWorker().getUserId());
        }

        @Test
        void offersAreSortedByDistanceClosestFirst() {
            Clock clock = Clock.systemDefaultZone();
            LocalDateTime locationNow = LocalDateTime.now(clock);

            // Booking 4 min old → radius = 5 km
            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(4), SERVICE_LAT, SERVICE_LON);

            // Workers at different distances within 5 km
            Worker far = createWorker(SERVICE_LAT + 0.04, SERVICE_LON, locationNow.minusMinutes(1));     // ~4.45 km
            Worker mid = createWorker(SERVICE_LAT + 0.02, SERVICE_LON, locationNow.minusMinutes(1));     // ~2.23 km
            Worker near = createWorker(SERVICE_LAT + 0.005, SERVICE_LON, locationNow.minusMinutes(1));   // ~0.56 km

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            // Return in unsorted order
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(far, near, mid));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, clock);

            assertEquals(3, service.discoverBooking(booking.getBookingId()));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BookingOffer>> captor = ArgumentCaptor.forClass(List.class);
            verify(bookingOfferRepository).saveAll(captor.capture());
            List<BookingOffer> offers = captor.getValue();
            assertEquals(3, offers.size());
            assertEquals(near.getUserId(), offers.get(0).getWorker().getUserId());
            assertEquals(mid.getUserId(), offers.get(1).getWorker().getUserId());
            assertEquals(far.getUserId(), offers.get(2).getWorker().getUserId());
        }

        @Test
        void workerExactlyAtRadiusBoundaryIsIncluded() {
            Clock clock = Clock.systemDefaultZone();
            LocalDateTime locationNow = LocalDateTime.now(clock);

            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusSeconds(30), SERVICE_LAT, SERVICE_LON);

            // lat offset giving distance < 2.0 km
            double latOffset = 1.99 / 111.32;
            Worker atBoundary = createWorker(SERVICE_LAT + latOffset, SERVICE_LON, locationNow.minusMinutes(1));

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(atBoundary));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, clock);

            assertEquals(1, service.discoverBooking(booking.getBookingId()));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Scenario 16: Boundary cases at each radius transition
    // ──────────────────────────────────────────────────────────────

    @Nested
    class RadiusTransitionIntegration {

        @Test
        void workerBetweenTwoAndFiveKmExcludedInitiallyIncludedAfterThreeMinutes() {
            Clock clock = Clock.systemDefaultZone();
            LocalDateTime locationNow = LocalDateTime.now(clock);

            // Worker at ~3 km (0.027 degrees latitude)
            Worker worker = createWorker(SERVICE_LAT + 0.027, SERVICE_LON, locationNow.minusMinutes(1));

            // Booking created 1 minute ago → radius = 2 km
            Booking recentBooking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(1), SERVICE_LAT, SERVICE_LON);
            when(bookingRepository.findByBookingIdForUpdate(recentBooking.getBookingId()))
                    .thenReturn(Optional.of(recentBooking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(worker));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, clock);

            // Excluded at 2 km radius
            assertEquals(0, service.discoverBooking(recentBooking.getBookingId()));

            // Booking created 4 minutes ago → radius = 5 km
            Booking olderBooking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(4), SERVICE_LAT, SERVICE_LON);
            when(bookingRepository.findByBookingIdForUpdate(olderBooking.getBookingId()))
                    .thenReturn(Optional.of(olderBooking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(worker));

            // Included at 5 km radius
            assertEquals(1, service.discoverBooking(olderBooking.getBookingId()));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Scenario 17: Booking expiry after 30 minutes
    // ──────────────────────────────────────────────────────────────

    @Nested
    class BookingExpiry {

        private final LocalDateTime fixedNow = LocalDateTime.of(2026, 6, 1, 12, 30, 0);
        private final Clock fixedClock = Clock.fixed(
                fixedNow.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        @Test
        void bookingJustBeforeThirtyMinuteExpiryTriggersDiscovery() {
            // Created 29 minutes and 59 seconds ago -> 1 second before 30-min boundary
            LocalDateTime createdAt = fixedNow.minusMinutes(30).plusSeconds(1);
            Booking booking = createBooking(BookingStatus.PENDING, createdAt, SERVICE_LAT, SERVICE_LON);

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of());

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, fixedClock);

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
            // Discovery proceeded because worker search was invoked
            verify(workerServiceRepository).findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        void bookingExactlyAtThirtyMinuteExpiryBoundaryDoesNotTriggerDiscovery() {
            // Created exactly 30 minutes ago -> at boundary (now.isBefore(createdAt + 30m) is false)
            LocalDateTime createdAt = fixedNow.minusMinutes(30);
            Booking booking = createBooking(BookingStatus.PENDING, createdAt, SERVICE_LAT, SERVICE_LON);

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, fixedClock);

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
            verify(workerServiceRepository, never()).findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        void bookingJustAfterThirtyMinuteExpiryBoundaryDoesNotTriggerDiscovery() {
            // Created 30 minutes and 1 second ago -> 1 second past 30-min boundary
            LocalDateTime createdAt = fixedNow.minusMinutes(30).minusSeconds(1);
            Booking booking = createBooking(BookingStatus.PENDING, createdAt, SERVICE_LAT, SERVICE_LON);

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, fixedClock);

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
            verify(workerServiceRepository, never()).findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        void bookingOlderThanThirtyMinutesDoesNotTriggerDiscovery() {
            LocalDateTime createdAt = fixedNow.minusMinutes(31);
            Booking booking = createBooking(BookingStatus.PENDING, createdAt, SERVICE_LAT, SERVICE_LON);

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, fixedClock);

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
            verify(workerServiceRepository, never()).findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Scenario 18: Acceptance and expiry race protection
    // ──────────────────────────────────────────────────────────────

    @Nested
    class AcceptanceExpiryRaceProtection {

        @Test
        void discoveryOnAlreadyAcceptedBookingReturnsZero() {
            Booking booking = createBooking(
                    BookingStatus.ACCEPTED,
                    LocalDateTime.now().minusMinutes(5), SERVICE_LAT, SERVICE_LON);
            Worker assignedWorker = createWorker(SERVICE_LAT, SERVICE_LON, LocalDateTime.now());
            booking.setWorker(assignedWorker);

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, Clock.systemDefaultZone());

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
            verify(workerServiceRepository, never()).findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        void discoveryOnPendingBookingWithWorkerAlreadySetReturnsZero() {
            Booking booking = createBooking(
                    BookingStatus.PENDING,
                    LocalDateTime.now().minusMinutes(2), SERVICE_LAT, SERVICE_LON);
            Worker assignedWorker = createWorker(SERVICE_LAT, SERVICE_LON, LocalDateTime.now());
            booking.setWorker(assignedWorker);

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, Clock.systemDefaultZone());

            assertEquals(0, service.discoverBooking(booking.getBookingId()));
        }

        @Test
        void discoveryUsesBookingIdForUpdateForPessimisticLocking() {
            UUID bookingId = UUID.randomUUID();
            when(bookingRepository.findByBookingIdForUpdate(bookingId))
                    .thenReturn(Optional.empty());

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, Clock.systemDefaultZone());

            assertThrows(IllegalArgumentException.class,
                    () -> service.discoverBooking(bookingId));

            verify(bookingRepository).findByBookingIdForUpdate(bookingId);
        }

        @Test
        void bookingNotFoundThrowsIllegalArgumentException() {
            UUID missingId = UUID.randomUUID();
            when(bookingRepository.findByBookingIdForUpdate(missingId))
                    .thenReturn(Optional.empty());

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, Clock.systemDefaultZone());

            assertThrows(IllegalArgumentException.class,
                    () -> service.discoverBooking(missingId));
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Haversine distance utility tests
    // ──────────────────────────────────────────────────────────────

    @Nested
    class DistanceCalculation {

        @Test
        void samePointReturnsZeroDistance() {
            assertEquals(0.0, BookingDiscoveryService.distanceKm(20.0, 85.0, 20.0, 85.0), 0.001);
        }

        @Test
        void knownDistanceBhubaneswarToCuttack() {
            double distance = BookingDiscoveryService.distanceKm(
                    20.2961, 85.8245,  // Bhubaneswar
                    20.4625, 85.8830); // Cuttack
            assertTrue(distance > 15.0 && distance < 25.0,
                    "Distance should be approximately 19 km, got: " + distance);
        }

        @Test
        void antipodalPointsReturnMaxEarthDistance() {
            double distance = BookingDiscoveryService.distanceKm(0.0, 0.0, 0.0, 180.0);
            assertTrue(distance > 20000.0 && distance < 20100.0,
                    "Antipodal distance should be ~20015 km, got: " + distance);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Multiple workers: verify exact worker IDs in saved offers
    // ──────────────────────────────────────────────────────────────

    @Nested
    class MultipleWorkerOfferVerification {

        @Test
        void multipleEligibleWorkersAllReceiveOffersWithCorrectIds() {
            Clock clock = Clock.systemDefaultZone();
            LocalDateTime locationNow = LocalDateTime.now(clock);

            // Booking 7 min old → radius = 8 km
            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(7), SERVICE_LAT, SERVICE_LON);

            Worker w1 = createWorker(SERVICE_LAT + 0.01, SERVICE_LON, locationNow.minusMinutes(1));
            Worker w2 = createWorker(SERVICE_LAT + 0.02, SERVICE_LON, locationNow.minusMinutes(1));
            Worker w3 = createWorker(SERVICE_LAT + 0.03, SERVICE_LON, locationNow.minusMinutes(1));

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(w3, w1, w2));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, clock);

            assertEquals(3, service.discoverBooking(booking.getBookingId()));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BookingOffer>> captor = ArgumentCaptor.forClass(List.class);
            verify(bookingOfferRepository).saveAll(captor.capture());
            List<BookingOffer> offers = captor.getValue();

            Set<UUID> offeredWorkerIds = offers.stream()
                    .map(o -> o.getWorker().getUserId())
                    .collect(Collectors.toSet());
            assertEquals(
                    Set.of(w1.getUserId(), w2.getUserId(), w3.getUserId()),
                    offeredWorkerIds);

            // Verify sorted closest first
            assertEquals(w1.getUserId(), offers.get(0).getWorker().getUserId());
            assertEquals(w2.getUserId(), offers.get(1).getWorker().getUserId());
            assertEquals(w3.getUserId(), offers.get(2).getWorker().getUserId());
        }

        @Test
        void allOffersReferenceTheCorrectBooking() {
            Clock clock = Clock.systemDefaultZone();
            LocalDateTime locationNow = LocalDateTime.now(clock);

            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(2), SERVICE_LAT, SERVICE_LON);
            Worker w1 = createWorker(SERVICE_LAT + 0.001, SERVICE_LON, locationNow.minusMinutes(1));
            Worker w2 = createWorker(SERVICE_LAT + 0.002, SERVICE_LON, locationNow.minusMinutes(1));

            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of(w1, w2));

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, clock);

            service.discoverBooking(booking.getBookingId());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BookingOffer>> captor = ArgumentCaptor.forClass(List.class);
            verify(bookingOfferRepository).saveAll(captor.capture());
            for (BookingOffer offer : captor.getValue()) {
                assertEquals(booking.getBookingId(), offer.getBooking().getBookingId());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Bounding box parameter verification
    // ──────────────────────────────────────────────────────────────

    @Nested
    class BoundingBoxParameters {

        @Test
        void repositoryReceivesCorrectBoundingBoxForTwoKmRadius() {
            Clock clock = Clock.systemDefaultZone();
            Booking booking = createBooking(
                    BookingStatus.PENDING, LocalDateTime.now().minusMinutes(1), SERVICE_LAT, SERVICE_LON);
            when(bookingRepository.findByBookingIdForUpdate(booking.getBookingId()))
                    .thenReturn(Optional.of(booking));
            when(workerServiceRepository.findEligibleWorkersForOffer(
                    any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(List.of());

            BookingDiscoveryService service = new BookingDiscoveryService(
                    bookingRepository, workerServiceRepository,
                    bookingOfferRepository, clock);
            service.discoverBooking(booking.getBookingId());

            double expectedLatDelta = 2.0 / 111.32;
            double expectedMinLat = SERVICE_LAT - expectedLatDelta;
            double expectedMaxLat = SERVICE_LAT + expectedLatDelta;

            ArgumentCaptor<Double> minLat = ArgumentCaptor.forClass(Double.class);
            ArgumentCaptor<Double> maxLat = ArgumentCaptor.forClass(Double.class);
            ArgumentCaptor<Double> minLon = ArgumentCaptor.forClass(Double.class);
            ArgumentCaptor<Double> maxLon = ArgumentCaptor.forClass(Double.class);

            verify(workerServiceRepository).findEligibleWorkersForOffer(
                    any(), any(), any(), any(),
                    minLat.capture(), maxLat.capture(),
                    minLon.capture(), maxLon.capture());

            assertEquals(expectedMinLat, minLat.getValue(), 0.0001);
            assertEquals(expectedMaxLat, maxLat.getValue(), 0.0001);
            assertTrue(minLon.getValue() < SERVICE_LON);
            assertTrue(maxLon.getValue() > SERVICE_LON);
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Helper methods
    // ────────────────────────────────────────────────────────────

    private Booking createBooking(BookingStatus status, LocalDateTime createdAt,
                                  double lat, double lon) {
        Service serviceEntity = new Service();
        serviceEntity.setServiceId(UUID.randomUUID());
        return createBookingWithServiceId(status, createdAt, lat, lon,
                serviceEntity.getServiceId());
    }

    private Booking createBookingWithServiceId(BookingStatus status, LocalDateTime createdAt,
                                               double lat, double lon, UUID serviceId) {
        Service serviceEntity = new Service();
        serviceEntity.setServiceId(serviceId);
        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setService(serviceEntity);
        booking.setStatus(status);
        booking.setServiceLatitude(BigDecimal.valueOf(lat));
        booking.setServiceLongitude(BigDecimal.valueOf(lon));
        if (createdAt != null) {
            setCreatedAt(booking, createdAt);
        }
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
