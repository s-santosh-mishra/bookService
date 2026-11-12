package com.servicehub.serv.service;

import com.servicehub.serv.dto.WorkerStatusDto;
import com.servicehub.serv.dto.WorkerLocationUpdateDto;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.exception.AccountStatusException;
import com.servicehub.serv.exception.ResourceNotFoundException;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WorkerStatusService {

    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES = List.of(
            BookingStatus.ACCEPTED,
            BookingStatus.IN_PROGRESS,
            BookingStatus.WAITING_FOR_PAYMENT);

    private final WorkerRepository workerRepository;
    private final BookingRepository bookingRepository;
    private final OdishaServiceArea odishaServiceArea;
    private final Clock clock;

    public WorkerStatusService(
            WorkerRepository workerRepository,
            BookingRepository bookingRepository,
            OdishaServiceArea odishaServiceArea,
            Clock clock) {
        this.workerRepository = workerRepository;
        this.bookingRepository = bookingRepository;
        this.odishaServiceArea = odishaServiceArea;
        this.clock = clock;
    }

    public WorkerStatusDto getWorkerStatus(UUID workerId) {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));

        return new WorkerStatusDto(
                worker.getVerificationStatus(),
                worker.getAvailabilityStatus()
        );
    }

    @Transactional
    public WorkerStatusDto goOnline(UUID workerId) {
        Worker worker = workerRepository.findByUserIdForUpdate(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));

        if (!worker.getUser().isActive()) {
            throw new AccountStatusException(
                    "Your account has been deactivated. Please contact support.");
        }

        if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new AccountStatusException(
                    "Only verified workers can go online.");
        }

        if (bookingRepository.existsByWorkerUserIdAndStatusIn(
                workerId,
                ACTIVE_BOOKING_STATUSES)) {
            throw new IllegalStateException(
                    "You cannot go online while you have an active booking.");
        }

        if (worker.getAvailabilityStatus() == AvailabilityStatus.BUSY) {
            throw new IllegalStateException(
                    "You cannot change to available while you have an active booking.");
        }

        worker.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        workerRepository.save(worker);

        return toStatusDto(worker);
    }

    @Transactional
    public WorkerStatusDto goOffline(UUID workerId) {
        Worker worker = workerRepository.findByUserIdForUpdate(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));

        if (worker.getAvailabilityStatus() == AvailabilityStatus.BUSY) {
            throw new IllegalStateException(
                    "You cannot go offline while you have an active booking.");
        }

        worker.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        workerRepository.save(worker);

        return toStatusDto(worker);
    }

    @Transactional
    public WorkerStatusDto updateLocation(
            UUID workerId,
            WorkerLocationUpdateDto request) {
        Worker worker = workerRepository.findByUserIdForUpdate(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));

        if (!worker.getUser().isActive()) {
            throw new AccountStatusException(
                    "Your account has been deactivated. Please contact support.");
        }

        if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new AccountStatusException(
                    "Only verified workers can update their location.");
        }

        if (worker.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Location can only be updated while you are online and available.");
        }

        if (request == null
                || request.getLatitude() == null
                || request.getLongitude() == null) {
            throw new IllegalArgumentException(
                    "Latitude and longitude are required.");
        }

        double latitude = request.getLatitude();
        double longitude = request.getLongitude();
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException(
                    "Latitude must be a finite value between -90 and 90.");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "Longitude must be a finite value between -180 and 180.");
        }
        if (!odishaServiceArea.contains(latitude, longitude)) {
            throw new IllegalArgumentException(
                    "Location must be within the Odisha service area.");
        }

        worker.setCurrentLatitude(BigDecimal.valueOf(latitude));
        worker.setCurrentLongitude(BigDecimal.valueOf(longitude));
        worker.setLocationLastUpdatedAt(LocalDateTime.now(clock));
        workerRepository.save(worker);

        return toStatusDto(worker);
    }

    public boolean hasFreshLocation(Worker worker) {
        return WorkerLocationFreshness.isFresh(
                worker,
                LocalDateTime.now(clock));
    }

    private WorkerStatusDto toStatusDto(Worker worker) {
        return new WorkerStatusDto(
                worker.getVerificationStatus(),
                worker.getAvailabilityStatus());
    }
}