package com.servicehub.serv.repository;

import com.servicehub.serv.entity.WorkerBookingRejection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkerBookingRejectionRepository
        extends JpaRepository<WorkerBookingRejection, UUID> {

    List<WorkerBookingRejection> findByWorker_UserId(UUID workerId);

    boolean existsByWorker_UserIdAndBooking_BookingId(
            UUID workerId,
            UUID bookingId
    );
}