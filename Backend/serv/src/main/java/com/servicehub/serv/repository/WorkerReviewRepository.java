package com.servicehub.serv.repository;

import com.servicehub.serv.entity.WorkerReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkerReviewRepository
        extends JpaRepository<WorkerReview, UUID> {

    boolean existsByBookingId(UUID bookingId);

    List<WorkerReview> findByWorkerId(UUID workerId);

    List<WorkerReview> findByWorkerIdOrderByCreatedAtDesc(UUID workerId);

    long countByWorkerId(UUID workerId);
}