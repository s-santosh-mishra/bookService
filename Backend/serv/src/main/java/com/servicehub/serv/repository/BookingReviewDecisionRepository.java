package com.servicehub.serv.repository;

import com.servicehub.serv.entity.BookingReviewDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingReviewDecisionRepository
        extends JpaRepository<BookingReviewDecision, UUID> {

    boolean existsByBookingId(UUID bookingId);
}