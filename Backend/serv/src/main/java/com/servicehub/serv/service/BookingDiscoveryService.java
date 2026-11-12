package com.servicehub.serv.service;

import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.BookingOffer;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.repository.BookingOfferRepository;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.WorkerServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class BookingDiscoveryService {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final BookingRepository bookingRepository;
    private final WorkerServiceRepository workerServiceRepository;
    private final BookingOfferRepository bookingOfferRepository;
    private final Clock locationClock;

    public BookingDiscoveryService(
            BookingRepository bookingRepository,
            WorkerServiceRepository workerServiceRepository,
            BookingOfferRepository bookingOfferRepository,
            Clock locationClock) {
        this.bookingRepository = bookingRepository;
        this.workerServiceRepository = workerServiceRepository;
        this.bookingOfferRepository = bookingOfferRepository;
        this.locationClock = locationClock;
    }

    @Transactional
    public int discoverBooking(UUID bookingId) {
        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        LocalDateTime bookingNow = LocalDateTime.now();
        if (booking.getStatus() != BookingStatus.PENDING
                || booking.getWorker() != null
                || booking.getCreatedAt() == null
                || !bookingNow.isBefore(booking.getCreatedAt().plusMinutes(30))) {
            return 0;
        }

        LocalDateTime locationNow = LocalDateTime.now(locationClock);
        double radiusKm = discoveryRadiusKm(booking.getCreatedAt(), bookingNow);
        double latitude = booking.getServiceLatitude().doubleValue();
        double longitude = booking.getServiceLongitude().doubleValue();
        double latitudeDelta = radiusKm / 111.32;
        double longitudeScale = Math.abs(Math.cos(Math.toRadians(latitude)));
        double longitudeDelta = longitudeScale < 1e-8
                ? 180.0
                : Math.min(180.0, radiusKm / (111.32 * longitudeScale));

        List<Worker> workers = workerServiceRepository.findEligibleWorkersForOffer(
                bookingId,
                booking.getService().getServiceId(),
                locationNow.minusMinutes(3),
                locationNow,
                Math.max(-90.0, latitude - latitudeDelta),
                Math.min(90.0, latitude + latitudeDelta),
                Math.max(-180.0, longitude - longitudeDelta),
                Math.min(180.0, longitude + longitudeDelta));

        List<WorkerDistance> eligibleWorkers = new ArrayList<>();
        for (Worker worker : workers) {
            if (!WorkerLocationFreshness.isFresh(worker, locationNow)) {
                continue;
            }
            double distanceKm = distanceKm(
                    latitude,
                    longitude,
                    worker.getCurrentLatitude().doubleValue(),
                    worker.getCurrentLongitude().doubleValue());
            if (distanceKm <= radiusKm) {
                eligibleWorkers.add(new WorkerDistance(worker, distanceKm));
            }
        }

        eligibleWorkers.sort(Comparator.comparingDouble(WorkerDistance::distanceKm));
        List<BookingOffer> offers = eligibleWorkers.stream()
                .map(candidate -> new BookingOffer(booking, candidate.worker()))
                .toList();
        if (offers.isEmpty()) {
            return 0;
        }
        bookingOfferRepository.saveAll(offers);
        return offers.size();
    }

    static double discoveryRadiusKm(LocalDateTime createdAt, LocalDateTime now) {
        long elapsedMinutes = Math.max(0, Duration.between(createdAt, now).toMinutes());
        if (elapsedMinutes < 3) {
            return 2.0;
        }
        if (elapsedMinutes < 6) {
            return 5.0;
        }
        if (elapsedMinutes < 9) {
            return 8.0;
        }
        return 10.0;
    }

    static double distanceKm(double latitude1, double longitude1, double latitude2, double longitude2) {
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude1))
                * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDelta / 2)
                * Math.sin(longitudeDelta / 2);
        double boundedHaversine = Math.max(0.0, Math.min(1.0, haversine));
        double centralAngle = 2 * Math.atan2(
                Math.sqrt(boundedHaversine),
                Math.sqrt(1.0 - boundedHaversine));
        return EARTH_RADIUS_KM * centralAngle;
    }

    private record WorkerDistance(Worker worker, double distanceKm) {
    }
}
